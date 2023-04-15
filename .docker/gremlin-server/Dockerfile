FROM tinkerpop/gremlin-server:3.6.2
USER gremlin
WORKDIR /opt/gremlin-server
COPY --chown=gremlin:gremlin ./conf/ /etc/config/
RUN bin/gremlin-server.sh install org.apache.tinkerpop neo4j-gremlin 3.6.2
CMD [ "/etc/config/gremlin-server.yaml" ]
