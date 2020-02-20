FROM maven:3.5-jdk-8-onbuild-alpine
CMD ["java", "-Xms32m", "-Xmx1g", "-jar","/usr/src/app/target/operator-estimator-jar-with-dependencies.jar"]