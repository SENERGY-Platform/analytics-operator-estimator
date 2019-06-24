FROM maven:3-jdk-11-slim
CMD ["java","-jar","/usr/src/app/target/operator-estimator-jar-with-dependencies.jar"]