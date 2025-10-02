FROM trafficserver/trafficserver:10.0.4

COPY remap.config /opt/etc/trafficserver/remap.config
COPY records.yaml /opt/etc/trafficserver/records.yaml

EXPOSE 8188

CMD ["traffic_server", "-K"]