package com.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {

    private final Vpc vpc;

    private final Cluster ecsCluster;

    public LocalStack(final App scope, final String id, final StackProps props){
        super(scope,id,props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDB =
                createDatabase("AuthServiceDB", "auth-service-db");

        DatabaseInstance patientServiceDB =
                createDatabase("PatientServiceDB", "patient-service-db");

        CfnHealthCheck authDBHealthCheck = createDbHealthCheck(authServiceDB, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDBHealthCheck = createDbHealthCheck(patientServiceDB, "PatientServiceDBHealthCheck");

        CfnCluster mskCluster = createMskCluster();

        this.ecsCluster = createEcsCluster();

        FargateService authService =
                createFargateService("AuthService","auth-service",
                        List.of(4005),
                        authServiceDB,
                        Map.of("JWT_SECRET","DqfRNKlqMHzn35r7nepsMafZDB37pFSHJCxOySeW7fW"));

        authService.getNode().addDependency(authDBHealthCheck);
        authService.getNode().addDependency(authServiceDB);


        FargateService billingService =
                createFargateService("BillingService",
                        "billing-service",
                        List.of(4001,9001),
                        null,
                        null);

        FargateService analyticsService =
                createFargateService("AnalyticsService",
                        "analytics-service",
                        List.of(4002),
                        null,
                        null);

        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService =
                createFargateService("PatientService",
                        "patient-service",
                        List.of(4000),
                        patientServiceDB,
                        Map.of(
                                "BILLING_SERVICE_ADDRESS","host.docker.internal",
                                "BILLING_SERVICE_GRPC_PORT","9001"
                        ));

        patientService.getNode().addDependency(patientServiceDB);
        patientService.getNode().addDependency(patientDBHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        createApiGatewayService();

    }

    private Vpc createVpc(){
        // Here this(scope) refers of the LocalStack which extends Stack
        return Vpc.Builder.create(this,"PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2) //means two availability zones(throughtout th world)
                .build();
    }


    private DatabaseInstance createDatabase(String id, String dbName){
        return DatabaseInstance.Builder
                .create(this,id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_17_2)
                        .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }


    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){

        return CfnHealthCheck.Builder.create(this,id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort())) // checks the port
                        .ipAddress(db.getDbInstanceEndpointAddress()) // checks the ip address
                        .requestInterval(30) //check all health endpoint every 30sec
                        .failureThreshold(3) // this health check tries 3 times
                        .build())
                .build();
    }

    private CfnCluster createMskCluster(){
        return CfnCluster.Builder.create(this,"MskCluster")
                .clusterName("Kafa-cluster")
                .kafkaVersion("3.6.0")
                .numberOfBrokerNodes(2)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .brokerAzDistribution("DEFAULT")
                        .build())
                .build();
    }

    private Cluster createEcsCluster(){
        return Cluster.Builder.create(this,"PatientManagementCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("patient-management.local")
                        .build())
                .build();

    }


    private FargateService createFargateService(String id,
                                                String imageName,
                                                List<Integer> ports,
                                                DatabaseInstance db,
                                                Map<String,String> additionalEnvVars ){

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this,id+"Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions.Builder continerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .portMappings(ports.stream()
                        .map(port-> PortMapping.builder()
                                .containerPort(port) // port that app is runnign on insid ethe container
                                .hostPort(port) // port that this container expose
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder() // to show the logs of the container
                        .logGroup(LogGroup.Builder.create(this,id+"LogGroup")
                                .logGroupName("/ecs/"+ imageName)
                                .removalPolicy(RemovalPolicy.DESTROY) // means whenever the stack is destoryed it is going to destroy thelogs
                                .retention(RetentionDays.ONE_DAY) // stores logs for how many days
                                .build())
                                .streamPrefix(imageName)
                        .build()));


        Map<String,String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if(additionalEnvVars != null){
            envVars.putAll(additionalEnvVars);
        }

        if(db != null){
            envVars.put("SPRING_DATASOURCE_URL","jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName
            ));

            envVars.put("SPRING_DATASOURCE_USERNAME","admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO","update");
            envVars.put("SPRING_SQL_INIT_MODE","always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT","60000");
        }

        continerOptions.environment(envVars);
        taskDefinition.addContainer(imageName+"Container", continerOptions.build());

        return FargateService.Builder.create(this,id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .build();
    }

    private void createApiGatewayService(){
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this,"APIGatewayTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        ContainerDefinitionOptions continerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("api-gateway"))
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "AUTH_SERVICE_URL","http://host.docker.internal:4005"
                ))
                .portMappings(List.of(4004).stream()
                        .map(port-> PortMapping.builder()
                                .containerPort(port) // port that app is runnign on insid ethe container
                                .hostPort(port) // port that this container expose
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder() // to show the logs of the container
                        .logGroup(LogGroup.Builder.create(this,"ApiGatewayLogGroup")
                                .logGroupName("/ecs/api-gateway")
                                .removalPolicy(RemovalPolicy.DESTROY) // means whenever the stack is destoryed it is going to destroy thelogs
                                .retention(RetentionDays.ONE_DAY) // stores logs for how many days
                                .build())
                        .streamPrefix("api-gateway")
                        .build()))
                .build();

        taskDefinition.addContainer("APIGatewayContainer",continerOptions);

        // THIS METHOD WILL AUTOMATICALLY CREATE AN APPLICATION LOAD BALANCER FOR US
        ApplicationLoadBalancedFargateService apiGateway
                = ApplicationLoadBalancedFargateService.Builder
                .create(this,"APIGatewayService")
                .cluster(ecsCluster)
                .serviceName("api-gateway")
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60))
                .build();


    }



    public static void main(final String[] args) {


        //creating a new aws cdk app and defining [outdir("./cdk.out"] where we want the output to be
        App app = new App(AppProps.builder().outdir("./cdk.out").build());


        // synthesizer(aws term) converts java code to cloud formation template
        // BootstraplessSynthesizer its tell cdk code to skip the initial bootstrping of the  cdk enviroment
        // as do not need it for the local stack
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();


        new LocalStack(app,"localstack",props);
        app.synth();
        System.out.println("App Synthesizing in progress...");
    }
}
