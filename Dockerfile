FROM maven:3.5-jdk-8-onbuild-alpine

ADD https://github.com/jmxtrans/jmxtrans-agent/releases/download/jmxtrans-agent-1.2.6/jmxtrans-agent-1.2.6.jar jmxtrans-agent.jar

CMD ["java", "-Xms32m", "-Xmx1g", "-jar","/usr/src/app/target/operator-estimator-jar-with-dependencies.jar"]