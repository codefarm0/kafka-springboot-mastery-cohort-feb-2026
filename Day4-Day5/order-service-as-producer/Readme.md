## create topic with isr config
```bash
bin/kafka-topics.sh --create \
  --topic orders \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 2 \
  --config min.insync.replicas=2
```
