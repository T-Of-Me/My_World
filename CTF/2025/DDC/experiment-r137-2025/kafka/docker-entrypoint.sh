#!/usr/bin/env bash
set -e
cat > /etc/kafka/kafka_server_jaas.conf << EOF
KafkaServer {
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="$KAFKA_RICK_USER"
    password="$KAFKA_RICK_PASSWORD"
    user_$KAFKA_RICK_USER="$KAFKA_RICK_PASSWORD"
    user_$KAFKA_MORTY_USER="$KAFKA_MORTY_PASSWORD";
};
EOF

/etc/confluent/docker/run &
KAFKA_PID=$!

sleep 15

cat > /tmp/rick.properties << EOF
sasl.mechanism=PLAIN
security.protocol=SASL_PLAINTEXT
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$KAFKA_RICK_USER" password="$KAFKA_RICK_PASSWORD";
EOF

kafka-acls --bootstrap-server localhost:9092 --command-config /tmp/rick.properties --add --allow-principal User:$KAFKA_MORTY_USER --operation Write --topic '*' &&
        
kafka-acls --bootstrap-server localhost:9092 --command-config /tmp/rick.properties --add --allow-principal User:$KAFKA_MORTY_USER --operation Describe --topic '*'

wait $KAFKA_PID