# Day 2: File System Connectors - Your First Connector

## Welcome to Your First Hands-On Experience

In Day 1, you learned about Kafka Connect architecture, workers, connectors, and tasks. Today, you'll build your first connectors using File Source and File Sink connectors. This hands-on experience will help you understand how connectors work in practice.

Why start with file system connectors? They're simple, visual, and easy to understand. You can see files moving from source to destination through Kafka, making it perfect for learning connector concepts.

---

## What We'll Learn Today

By the end of this day, you'll be able to:

- Set up Kafka Connect in distributed mode using Docker Compose
- Install and configure File Source and Sink connectors
- Create connectors via REST API
- Implement direct file-to-file migration through Kafka
- Add transformations using Single Message Transforms (SMT)
- Monitor connectors using REST API
- Troubleshoot common connector issues

---

## Prerequisites

Before we start, ensure you have:

- Docker and Docker Compose installed
- Basic understanding of Kafka topics and partitions
- Familiarity with REST API (curl commands)
- Terminal/command line access

---

## Setting Up Kafka Connect

### Docker Compose Setup

We'll use Docker Compose to set up a complete Kafka Connect environment with Kafka 4.x (KRaft mode, no Zookeeper). We'll use Confluent Community images which are open source and fully support KRaft mode. Create a `docker-compose.yml` file:

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.6.0
    container_name: kafka
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
      KAFKA_LOG_DIRS: /var/lib/kafka/data

  kafka-connect:
    image: confluentinc/cp-kafka-connect:7.6.0
    container_name: kafka-connect
    depends_on:
      - kafka
    ports:
      - "8083:8083"
    environment:
      CONNECT_BOOTSTRAP_SERVERS: kafka:9092
      CONNECT_REST_ADVERTISED_HOST_NAME: localhost
      CONNECT_REST_PORT: 8083
      CONNECT_GROUP_ID: connect-cluster
      CONNECT_CONFIG_STORAGE_TOPIC: connect-configs
      CONNECT_OFFSET_STORAGE_TOPIC: connect-offsets
      CONNECT_STATUS_STORAGE_TOPIC: connect-status
      CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR: 1
      CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR: 1
      CONNECT_STATUS_STORAGE_REPLICATION_FACTOR: 1
      CONNECT_KEY_CONVERTER: org.apache.kafka.connect.storage.StringConverter
      CONNECT_VALUE_CONVERTER: org.apache.kafka.connect.storage.StringConverter
      CONNECT_INTERNAL_KEY_CONVERTER: org.apache.kafka.connect.storage.StringConverter
      CONNECT_INTERNAL_VALUE_CONVERTER: org.apache.kafka.connect.storage.StringConverter
      CONNECT_PLUGIN_PATH: /usr/share/java,/usr/share/confluent-hub-components
    volumes:
      - ./source-files:/source-files
      - ./destination-files:/destination-files

  kafdrop:
    image: obsidiandynamics/kafdrop:4.0.1
    container_name: kafdrop
    depends_on:
      - kafka
    ports:
      - "9000:9000"
    environment:
      KAFKA_BROKERCONNECT: kafka:9092
      SERVER_SERVLET_CONTEXTPATH: "/"
```

### Starting the Environment

Start all services:

```bash
docker-compose up -d
```

Wait for services to be ready (about 30-60 seconds), then verify Kafka Connect is running:

```bash
curl http://localhost:8083/connector-plugins
```

You should see a JSON response with available connector plugins. The File Source and File Sink connectors are built-in, so they should appear in the list.

### Access Kafdrop UI

Kafdrop provides a web UI for Kafka. Open your browser and navigate to:

```
http://localhost:9000
```

Or access it directly: [http://localhost:9000](http://localhost:9000)

You can use Kafdrop to:
- View all Kafka topics
- Browse messages in topics
- View consumer groups
- Monitor connector topics (connect-configs, connect-offsets, connect-status)

### Verify Internal Topics

Kafka Connect creates internal topics for coordination. Verify they exist:

```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

You should see:
- `connect-configs`
- `connect-offsets`
- `connect-status`

Alternatively, check Kafdrop UI at http://localhost:9000 to see these topics.

---

## Understanding File Connectors

### File Source Connector

The File Source Connector reads files from a directory and publishes each line as a Kafka record.

**How it works**:
- Monitors a directory for new files
- Reads files line by line
- Publishes each line as a separate Kafka record
- Tracks which files and lines have been processed (offset management)
- Supports file rotation and cleanup

**Key Configuration Properties**:
- `file`: Path to file or directory to read from
- `topic`: Kafka topic to publish records to
- `tasks.max`: Number of parallel tasks (usually 1 for file source)

### File Sink Connector

The File Sink Connector consumes records from Kafka topics and writes them to files.

**How it works**:
- Consumes records from Kafka topics
- Writes records to files in a directory
- Handles file rotation based on size or time
- Supports different output formats

**Key Configuration Properties**:
- `topics`: Kafka topics to consume from
- `file`: Output file path
- `tasks.max`: Number of parallel tasks

---

## Hands-On: Direct File Migration

Let's implement a simple file-to-file migration through Kafka. This demonstrates the basic source and sink connector pattern. We'll work with a CSV file containing product information.

### Step 1: Prepare Source Files

Create a source directory and add a CSV file with product data:

```bash
mkdir -p source-files
```

Create `source-files/products.csv` with the following content:

```csv
product_id,product_name,category,price,stock_quantity,description,supplier,rating,created_date
1001,Wireless Mouse,Electronics,29.99,150,Ergonomic wireless mouse with 2.4GHz connectivity,TechSupplies Inc,4.5,2024-01-15
1002,Mechanical Keyboard,Electronics,89.99,75,Cherry MX switches with RGB backlighting,TechSupplies Inc,4.7,2024-01-16
1003,USB-C Hub,Electronics,45.99,200,7-in-1 USB-C hub with HDMI and SD card reader,GlobalTech,4.3,2024-01-17
1004,Standing Desk,Office Furniture,299.99,50,Adjustable height standing desk with memory presets,OfficePro,4.6,2024-01-18
1005,Monitor Stand,Office Furniture,79.99,120,Aluminum monitor stand with cable management,OfficePro,4.4,2024-01-19
1006,Webcam HD,Electronics,59.99,90,1080p HD webcam with auto-focus and noise cancellation,TechSupplies Inc,4.5,2024-01-20
1007,Desk Lamp,Office Furniture,34.99,180,LED desk lamp with adjustable brightness and color temperature,HomeOffice,4.2,2024-01-21
1008,USB Flash Drive 64GB,Electronics,12.99,300,USB 3.0 flash drive with 64GB storage capacity,GlobalTech,4.6,2024-01-22
1009,Wireless Headphones,Electronics,129.99,60,Noise-cancelling wireless headphones with 30-hour battery,AudioTech,4.8,2024-01-23
1010,Desk Organizer,Office Furniture,24.99,250,Multi-compartment desk organizer with pen holders,HomeOffice,4.1,2024-01-24
```

You can create this file using:

```bash
cat > source-files/products.csv << 'EOF'
product_id,product_name,category,price,stock_quantity,description,supplier,rating,created_date
1001,Wireless Mouse,Electronics,29.99,150,Ergonomic wireless mouse with 2.4GHz connectivity,TechSupplies Inc,4.5,2024-01-15
1002,Mechanical Keyboard,Electronics,89.99,75,Cherry MX switches with RGB backlighting,TechSupplies Inc,4.7,2024-01-16
1003,USB-C Hub,Electronics,45.99,200,7-in-1 USB-C hub with HDMI and SD card reader,GlobalTech,4.3,2024-01-17
1004,Standing Desk,Office Furniture,299.99,50,Adjustable height standing desk with memory presets,OfficePro,4.6,2024-01-18
1005,Monitor Stand,Office Furniture,79.99,120,Aluminum monitor stand with cable management,OfficePro,4.4,2024-01-19
1006,Webcam HD,Electronics,59.99,90,1080p HD webcam with auto-focus and noise cancellation,TechSupplies Inc,4.5,2024-01-20
1007,Desk Lamp,Office Furniture,34.99,180,LED desk lamp with adjustable brightness and color temperature,HomeOffice,4.2,2024-01-21
1008,USB Flash Drive 64GB,Electronics,12.99,300,USB 3.0 flash drive with 64GB storage capacity,GlobalTech,4.6,2024-01-22
1009,Wireless Headphones,Electronics,129.99,60,Noise-cancelling wireless headphones with 30-hour battery,AudioTech,4.8,2024-01-23
1010,Desk Organizer,Office Furniture,24.99,250,Multi-compartment desk organizer with pen holders,HomeOffice,4.1,2024-01-24
EOF
```

### Step 2: Create File Source Connector

Create a file `file-source-connector.json`:

```json
{
  "name": "file-source-connector",
  "config": {
    "connector.class": "org.apache.kafka.connect.file.FileStreamSourceConnector",
    "tasks.max": "1",
    "file": "/source-files/products.csv",
    "topic": "products-topic"
  }
}
```

Deploy the connector:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @file-source-connector.json
```

### Step 3: Verify Source Connector

Check connector status:

```bash
curl http://localhost:8083/connectors/file-source-connector/status
```

Expected response:

```json
{
  "name": "file-source-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "localhost:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "localhost:8083"
    }
  ]
}
```

Verify records in Kafka topic:

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic products-topic \
  --from-beginning
```

You should see each line from `products.csv` appearing as a separate record in the topic. Each CSV row becomes one Kafka message.

Alternatively, you can view the messages in Kafdrop UI:
1. Open http://localhost:9000 in your browser
2. Click on the `products-topic` topic
3. View the messages - you'll see each CSV row as a separate message

### Step 4: Create File Sink Connector

Create a file `file-sink-connector.json`:

```json
{
  "name": "file-sink-connector",
  "config": {
    "connector.class": "org.apache.kafka.connect.file.FileStreamSinkConnector",
    "tasks.max": "1",
    "topics": "products-topic",
    "file": "/destination-files/products-output.csv"
  }
}
```

Deploy the connector:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @file-sink-connector.json
```

### Step 5: Verify Complete Migration

Check the destination file:

```bash
cat destination-files/products-output.csv
```

You should see all the product records from the source CSV file written to the destination file. The complete flow is:

```
source-files/products.csv → products-topic → destination-files/products-output.csv
```

Each CSV row from the source file is read by the source connector, published to Kafka as a message, consumed by the sink connector, and written to the destination file.

You can also verify in Kafdrop:
1. Check the `products-topic` to see all messages
2. Verify the consumer group `connect-file-sink-connector` is consuming messages
3. Check the offset progress to confirm all messages are processed

### Step 6: Test with New Data

Add new product records to the CSV file:

```bash
echo "1011,Wireless Charger,Electronics,24.99,100,Qi-compatible wireless charging pad,TechSupplies Inc,4.4,2024-01-25" >> source-files/products.csv
echo "1012,Desk Mat,Office Furniture,19.99,200,Large desk mat with mouse pad area,HomeOffice,4.3,2024-01-26" >> source-files/products.csv
```

**Important Note**: The File Source Connector reads files once and tracks what it has read. To process new lines added to an existing file, you have a few options:

1. **Delete and recreate the connector** (resets offset):
```bash
curl -X DELETE http://localhost:8083/connectors/file-source-connector
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @file-source-connector.json
```

2. **Use a new file** and update the connector configuration:
```bash
cp source-files/products.csv source-files/products-updated.csv
# Add new records to products-updated.csv
```

Then update the connector to read from the new file:

```json
{
  "name": "file-source-connector",
  "config": {
    "connector.class": "org.apache.kafka.connect.file.FileStreamSourceConnector",
    "tasks.max": "1",
    "file": "/source-files/products-updated.csv",
    "topic": "products-topic"
  }
}
```

Update the connector:

```bash
curl -X PUT http://localhost:8083/connectors/file-source-connector/config \
  -H "Content-Type: application/json" \
  -d @file-source-connector.json
```

The new product records should flow through Kafka to the destination file.

---

## Understanding Single Message Transforms (SMT)

Single Message Transforms allow you to modify records as they flow through connectors. SMTs are applied in a pipeline, where each transform processes the output of the previous one.

**Common Use Cases**:
- Add or modify record headers
- Filter records based on conditions
- Extract fields from values
- Convert data formats
- Add metadata (timestamps, source information)

**SMT Configuration**:
SMTs are configured using the `transforms` property in connector configuration:

```json
{
  "transforms": "transform1,transform2",
  "transforms.transform1.type": "org.apache.kafka.connect.transforms.ReplaceField$Value",
  "transforms.transform1.renames": "oldField:newField"
}
```

---

## Hands-On: Adding Transformations

Let's enhance our CSV product migration with transformations. We'll add metadata and filter products based on criteria.

### Step 1: Enhanced Source Connector with Metadata

Create `file-source-with-transform.json`:

```json
{
  "name": "file-source-transform-connector",
  "config": {
    "connector.class": "org.apache.kafka.connect.file.FileStreamSourceConnector",
    "tasks.max": "1",
    "file": "/source-files/products.csv",
    "topic": "products-transform-topic",
    "transforms": "addTimestamp,addSource",
    "transforms.addTimestamp.type": "org.apache.kafka.connect.transforms.InsertField$Value",
    "transforms.addTimestamp.timestamp.field": "processed_at",
    "transforms.addSource.type": "org.apache.kafka.connect.transforms.InsertField$Value",
    "transforms.addSource.static.field": "source_system",
    "transforms.addSource.static.value": "file-connector"
  }
}
```

This configuration:
- Adds a `processed_at` timestamp field to each CSV record
- Adds a static `source_system` field identifying the connector

Deploy the connector:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @file-source-with-transform.json
```

### Step 2: Enhanced Sink Connector with Header Addition

Create `file-sink-with-transform.json`:

```json
{
  "name": "file-sink-transform-connector",
  "config": {
    "connector.class": "org.apache.kafka.connect.file.FileStreamSinkConnector",
    "tasks.max": "1",
    "topics": "products-transform-topic",
    "file": "/destination-files/products-transformed.csv",
    "transforms": "addHeader",
    "transforms.addHeader.type": "org.apache.kafka.connect.transforms.InsertHeader",
    "transforms.addHeader.header": "X-Processed-By",
    "transforms.addHeader.value.literal": "kafka-connect"
  }
}
```

### Step 3: Working with CSV Data and Filtering

For more advanced transformations with CSV data, you can use Kafka Streams or process the data in your application. However, with basic SMTs, you can:

1. **Add metadata to each record** (as shown above)
2. **Add headers** to messages
3. **Extract specific fields** from the CSV line

Create a connector that adds processing metadata:

```json
{
  "name": "csv-products-enhanced",
  "config": {
    "connector.class": "org.apache.kafka.connect.file.FileStreamSourceConnector",
    "tasks.max": "1",
    "file": "/source-files/products.csv",
    "topic": "products-enhanced-topic",
    "transforms": "addProcessingInfo",
    "transforms.addProcessingInfo.type": "org.apache.kafka.connect.transforms.InsertField$Value",
    "transforms.addProcessingInfo.static.field": "processing_info",
    "transforms.addProcessingInfo.static.value": "enhanced-with-metadata"
  }
}
```

**Note**: For complex CSV parsing, field extraction, or filtering based on CSV column values, you would typically:
- Use a CSV-specific connector (like JDBC Source with CSV import)
- Process CSV in your application before sending to Kafka
- Use Kafka Streams for real-time CSV processing and filtering

The File Source Connector treats each CSV line as a string, so advanced CSV operations require additional processing.

---

## Monitoring Connectors

### REST API Endpoints

Kafka Connect provides REST API endpoints for monitoring:

**List all connectors**:
```bash
curl http://localhost:8083/connectors
```

**Get connector status**:
```bash
curl http://localhost:8083/connectors/file-source-connector/status
```

**Get connector configuration**:
```bash
curl http://localhost:8083/connectors/file-source-connector/config
```

**Get connector tasks**:
```bash
curl http://localhost:8083/connectors/file-source-connector/tasks
```

**Get task status**:
```bash
curl http://localhost:8083/connectors/file-source-connector/tasks/0/status
```

### Understanding Status Responses

A connector status response includes:

- **Connector State**: RUNNING, PAUSED, FAILED, UNASSIGNED
- **Task States**: Individual task status and worker assignment
- **Worker ID**: Which worker is running the connector/task

**Example Status Response**:
```json
{
  "name": "file-source-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "localhost:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "localhost:8083"
    }
  ]
}
```

### Monitoring Data Flow

Check if records are being produced:

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic products-topic \
  --from-beginning \
  --max-messages 10
```

Check consumer lag (for sink connectors):

```bash
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group connect-file-sink-connector \
  --describe
```

### Using Kafdrop for Monitoring

Kafdrop provides a visual interface for monitoring:

1. **View Topics**: Navigate to http://localhost:9000 and see all topics including:
   - `products-topic` (your data topic)
   - `connect-configs`, `connect-offsets`, `connect-status` (internal topics)

2. **Browse Messages**: Click on any topic to view messages:
   - See message keys and values
   - View message headers
   - Check partition and offset information

3. **Monitor Consumer Groups**: View consumer groups and their lag:
   - `connect-file-sink-connector` - your sink connector consumer group
   - Check if all messages are being consumed

4. **View Connector Topics**: Check internal Connect topics:
   - `connect-configs`: Stores connector configurations
   - `connect-offsets`: Tracks source connector offsets
   - `connect-status`: Stores connector and task status

---

## Connector Lifecycle Management

### Pausing and Resuming Connectors

Pause a connector (stops processing but retains configuration):

```bash
curl -X PUT http://localhost:8083/connectors/file-source-connector/pause
```

Resume a paused connector:

```bash
curl -X PUT http://localhost:8083/connectors/file-source-connector/resume
```

### Restarting Connectors

Restart a connector:

```bash
curl -X POST http://localhost:8083/connectors/file-source-connector/restart
```

Restart a specific task:

```bash
curl -X POST http://localhost:8083/connectors/file-source-connector/tasks/0/restart
```

### Deleting Connectors

Delete a connector:

```bash
curl -X DELETE http://localhost:8083/connectors/file-source-connector
```

**Note**: Deleting a connector removes its configuration but does not delete the Kafka topics or data.

---

## Troubleshooting Common Issues

### Issue 1: Connector in FAILED State

**Symptoms**: Connector status shows `"state": "FAILED"`

**Common Causes**:
- Invalid configuration
- File path doesn't exist
- Permission issues
- Invalid topic name

**Solution**:
1. Check connector status for error messages:
```bash
curl http://localhost:8083/connectors/file-source-connector/status
```

2. Check Kafka Connect logs:
```bash
docker-compose logs kafka-connect
```

3. Verify configuration:
```bash
curl http://localhost:8083/connectors/file-source-connector/config
```

4. Fix the issue and restart the connector

### Issue 2: No Records Appearing in Topic

**Symptoms**: Connector is RUNNING but no records in Kafka topic

**Common Causes**:
- Source file is empty
- File path incorrect
- Connector already processed the file (offset stored)

**Solution**:
1. Verify file exists and has content:
```bash
docker-compose exec kafka-connect ls -la /source-files/
docker-compose exec kafka-connect cat /source-files/file1.txt
```

2. Check if offset exists (connector remembers what it read):
   - Delete and recreate the connector to reset offsets
   - Or modify the file to trigger new reads

3. Verify topic exists:
```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Issue 3: Sink Connector Not Writing Files

**Symptoms**: Sink connector RUNNING but no output files

**Common Causes**:
- Destination directory doesn't exist
- Permission issues
- No records in source topic

**Solution**:
1. Verify destination directory exists:
```bash
docker-compose exec kafka-connect ls -la /destination-files/
```

2. Check if records exist in topic:
```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic file-source-topic \
  --from-beginning \
  --max-messages 1
```

3. Check consumer group offset:
```bash
docker-compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group connect-file-sink-connector \
  --describe
```

### Issue 4: Transformation Errors

**Symptoms**: Connector fails when SMT is applied

**Common Causes**:
- Invalid SMT configuration
- SMT class not available
- Data format mismatch

**Solution**:
1. Verify SMT class name is correct
2. Check SMT documentation for required configuration
3. Test without SMT first, then add transforms incrementally

---

## Best Practices

- **Use absolute paths**: File paths in Docker should be absolute and match volume mounts
- **Monitor connector status**: Regularly check connector and task status
- **Handle file rotation**: File source connector processes files once; for continuous monitoring, consider directory-based approaches
- **Test transformations incrementally**: Add one transform at a time to isolate issues
- **Clean up test connectors**: Delete connectors when done to avoid confusion
- **Use appropriate task count**: File connectors typically use `tasks.max: 1` since files are processed sequentially
- **Verify file permissions**: Ensure Kafka Connect container can read source files and write destination files

---

## Summary

Today you learned:

- How to set up Kafka Connect in distributed mode using Docker Compose
- How to create and deploy File Source and Sink connectors via REST API
- How to implement direct file-to-file migration through Kafka
- How to add transformations using Single Message Transforms (SMT)
- How to monitor connectors using REST API
- How to troubleshoot common connector issues

**Key Concepts**:
- File Source Connector reads files and publishes to Kafka topics
- File Sink Connector consumes from topics and writes to files
- SMTs allow data transformation during connector processing
- Connector lifecycle: create, pause, resume, restart, delete
- Monitoring via REST API provides visibility into connector health

---

## Next Steps

**Day 3: Database Integration with JDBC Connectors**
- Set up JDBC source and sink connectors
- Implement database table synchronization
- Understand offset modes (incrementing, timestamp, timestamp+incrementing)
- Handle schema evolution
- Configure error handling and dead letter topics

**Coming Up**:
- **Day 4**: Debezium CDC - Real-time change capture
- **Day 5**: Elasticsearch & Search Integration
- **Day 6**: Custom Connector Development

---

## Resources

- [Kafka Connect File Connector Documentation](https://kafka.apache.org/documentation/#connect_file_source)
- [Single Message Transforms Guide](https://docs.confluent.io/platform/current/connect/transforms/overview.html)
- [Kafka Connect REST API Reference](https://docs.confluent.io/platform/current/connect/references/restapi.html)

---

## Conclusion

You've successfully built your first Kafka Connect connectors! File system connectors provide a simple, visual way to understand how connectors work. The patterns you learned today - source connectors reading data, sink connectors writing data, and transformations in between - apply to all connector types.

In the next days, you'll apply these same concepts to databases, search engines, and eventually build your own custom connectors.

