FROM openjdk:8-jre-alpine

COPY app.war /usr/app/

COPY app.jar /usr/app/

WORKDIR /usr/app

CMD java -jar app.jar app.war