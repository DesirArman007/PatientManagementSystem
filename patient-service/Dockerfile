#use maven with jsk-21 as the base image for building, name this stage builder
FROM maven:3.9.9-eclipse-temurin-21 AS builder

#set the working directory inside the container to /app(it will be created if missing)
WORKDIR /app

#copy only the maven configuration file first
COPY pom.xml .

#download and cache all the dependencies to speed up the future builds
RUN mvn dependency:go-offline -B

#copy the actual source code into the conatiner
COPY src ./src

#build the project and package it into a jar file
RUN mvn clean package

#starts a new docker build stage
#uses the official openjdk image with jdk-21
#this stage will run your app(maven is no longer needed)
FROM openjdk:21-jdk AS runner

#sets the working directory inside conatiner to /app
#if it doesn't exist the docker will create it
WORKDIR /app

#copies the packeged JAR file from earlier builder stage to runner stage
# SOURCE: .app/target/patient-service-0.0.1-SNAPSHOT.jar
#Destination: ./app.jar
COPY --from=builder app/target/patient-service-0.0.1-SNAPSHOT.jar ./app.jar

#application inside the container listens on port
EXPOSE 4000

#define the startup command for the container
ENTRYPOINT ["java","-jar","app.jar"]


