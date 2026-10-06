FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY SmartCityJava/pom.xml SmartCityJava/pom.xml
COPY SmartCityJava/src SmartCityJava/src
COPY public public
COPY uploads uploads
RUN mvn -B -f SmartCityJava/pom.xml clean package

FROM tomcat:10.1-jdk17-temurin

ENV SMARTCITY_ENV=production
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/SmartCityJava/target/SmartCity.war /usr/local/tomcat/webapps/ROOT.war
COPY docker/start-tomcat.sh /usr/local/bin/start-tomcat.sh
RUN chmod +x /usr/local/bin/start-tomcat.sh

EXPOSE 8080
CMD ["/usr/local/bin/start-tomcat.sh"]
