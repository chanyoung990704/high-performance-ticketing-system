#!/bin/bash

# Kafka Consumer Lag 모니터링 스크립트
echo ">>> Checking Kafka Consumer Lag for 'booking-processor' group..."

docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group booking-processor \
  --describe

echo ">>> Checking Kafka Consumer Lag for 'dlq-processor' group..."

docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group dlq-processor \
  --describe
