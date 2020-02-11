#!/usr/bin/env bash
echo "docker exec -it  kafka-docker_kafka_1 bash /opt/kafka_2.12-2.3.0/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic villager --group broker-consumer-2 --property print.key=true  --property key.separator="-""
docker exec -it  kafka-docker_kafka_1 bash /opt/kafka_2.12-2.3.0/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic villager--group broker-consumer-2 --property print.key=true  --property key.separator="-"
