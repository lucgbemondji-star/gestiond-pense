FROM maven:3.9.9-eclipse-temurin-17 AS compilation

WORKDIR /projet
COPY pom.xml .
COPY schema.sql ./schema.sql
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM tomcat:10.1-jdk17-temurin

COPY docker/server.xml.template /usr/local/tomcat/conf/server.xml.template
COPY docker/server-entrypoint.sh /usr/local/bin/server-entrypoint.sh
COPY --from=compilation /projet/target/gestionnaire-depenses-1.0.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

RUN chmod 755 /usr/local/bin/server-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/server-entrypoint.sh"]
