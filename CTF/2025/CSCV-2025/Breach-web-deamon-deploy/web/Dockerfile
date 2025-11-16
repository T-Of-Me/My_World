FROM tomcat:10.1-jdk17

RUN groupadd -r tomcat && useradd -r -g tomcat tomcat
RUN rm -rf /usr/local/tomcat/webapps/*
RUN rm -rf /usr/local/tomcat/webapps.dist
COPY web/ROOT /usr/local/tomcat/webapps/ROOT

RUN chmod 775 -R /usr/local/tomcat/webapps/
RUN chmod 777 -R /usr/local/tomcat/webapps/ROOT/home/sub/

USER tomcat
EXPOSE 8080

CMD ["catalina.sh", "run"]