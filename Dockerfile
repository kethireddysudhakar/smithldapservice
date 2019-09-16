From tomcat:8.0.51-jre8-alpine
RUN rm -rf /usr/local/tomcat/webapps/*
COPY ./target/SmithLDAPServices-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
RUN sed -i 's/port="8080"/port="8082"/' /usr/local/tomcat/conf/server.xml
CMD ["catalina.sh","run"]

# Make port 8082 available to the world outside this container
EXPOSE 8082