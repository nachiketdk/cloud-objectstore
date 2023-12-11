## Use an official JDK 7 runtime as a base image
#FROM maven:3.8.3-openjdk-17 AS build
#COPY src /home/app/src
#COPY pom.xml /home/app
#RUN mvn -f /home/app/pom.xml -Dmaven.test.skip package
#
## Set the working directory
##WORKDIR /usr/src/app
#
## Expose the port your application will run on (adjust if needed)
#EXPOSE 8080
#
## Command to run your application
#ENTRYPOINT ["java", "-jar", "/home/app/target/load_balancer.war"]