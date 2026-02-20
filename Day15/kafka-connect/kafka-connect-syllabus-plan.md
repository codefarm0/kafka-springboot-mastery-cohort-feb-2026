# Kafka Connect Mastery: Complete Syllabus Plan

## Overview

This 6-day course takes you from Kafka Connect fundamentals to building custom connectors. Each day builds on the previous one, with hands-on implementations and real-world patterns.

**Target Audience**: Developers who understand Kafka producers/consumers and want to master data integration  
**Format**: 8-10 minute read + hands-on demo per day  
**Style**: Core engineering, no fluff, production-ready patterns

---

## Day 1: Kafka Connect Fundamentals & Architecture

### Learning Objectives
- Understand what Kafka Connect is and why it exists
- Learn Kafka Connect architecture (workers, connectors, tasks)
- Understand source vs sink connectors conceptually
- Know when to use Kafka Connect vs custom code vs Kafka Streams
- Understand distributed vs standalone modes
- Learn connector lifecycle and offset management concepts
- Understand error handling strategies (high-level)

---

## Day 2: File System Connectors - Your First Connector

### Learning Objectives
- Set up Kafka Connect in distributed mode
- Install and configure File Source and Sink connectors
- Implement file-to-file migration through Kafka
- Understand connector configuration basics
- Monitor connectors via REST API
- Implement data transformations using Single Message Transform (SMT)
- Handle file formats (JSON, CSV, text)
- Understand connector lifecycle and task management

### Hands-On Demo
- Set up Kafka Connect cluster
- Create File Source connector to read from source directory
- Create File Sink connector to write to destination directory
- Direct migration: Files from source → Kafka → destination (no transformation)
- Enhanced migration: Add transformations (format conversion, field mapping, filtering)
- Monitor connector status and data flow

---

## Day 3: Database Integration with JDBC Connectors

### Learning Objectives
- Install and configure JDBC connectors
- Implement JDBC source connector (table mode, query mode)
- Implement JDBC sink connector
- Understand offset modes (incrementing, timestamp, timestamp+incrementing)
- Handle schema evolution
- Configure error handling and dead letter topics
- Monitor connectors via REST API

### Hands-On Demo
- Create JDBC source connector for orders table
- Create JDBC sink connector to data warehouse
- Monitor connector status
- Handle errors and verify DLQ

---

## Day 4: Debezium CDC - Change Data Capture Deep Dive

### Learning Objectives
- Understand Change Data Capture (CDC) concepts
- Set up Debezium MySQL connector
- Configure binlog reading
- Handle schema changes
- Process change events (insert, update, delete)
- Understand Debezium event structure
- Replace outbox pattern with CDC
- Handle transaction boundaries

### Hands-On Demo
- Configure MySQL for binlog
- Set up Debezium MySQL connector
- Capture order table changes
- Process change events in Spring Boot consumer
- Handle schema evolution
- Replace manual outbox polling

---

## Day 5: Elasticsearch & Search Integration

### Learning Objectives
- Set up Elasticsearch Sink connector
- Configure topic-to-index mapping
- Handle document IDs and routing
- Implement batch indexing
- Handle schema and transformations
- Monitor indexing performance
- Handle errors and retries
- Optimize for search use cases

### Hands-On Demo
- Set up Elasticsearch cluster
- Create Elasticsearch sink connector
- Index order events
- Configure batch processing
- Monitor indexing performance
- Handle errors and verify DLQ

---

## Day 6: Custom Connector Development

### Learning Objectives
- Understand connector API (SourceConnector, SinkConnector)
- Implement custom source connector
- Implement custom sink connector
- Handle configuration validation
- Implement task splitting
- Handle offsets (for source)
- Implement error handling
- Test connectors
- Package and deploy connectors

### Hands-On Demo
- Build custom REST API source connector
- Build custom notification sink connector
- Test connectors
- Package and deploy
- Monitor custom connectors

---

## Project Integration

Throughout the course, we'll extend the e-commerce system:

- **Day 2**: File system migration patterns (foundation for understanding connectors)
- **Day 3**: Replace manual database polling with JDBC source connector
- **Day 4**: Replace outbox pattern with Debezium CDC
- **Day 5**: Index events in Elasticsearch for search
- **Day 6**: Build custom connector for specific integration needs

---


## Resources

- [Kafka Connect Documentation](https://kafka.apache.org/documentation/#connect)
- [Confluent Hub (connector marketplace)](https://www.confluent.io/hub/)
- [Debezium Documentation](https://debezium.io/documentation/)
- [Elasticsearch Connector Guide](https://docs.confluent.io/current/connect/kafka-connect-elasticsearch/index.html)
- [Kafka Connect REST API Reference](https://docs.confluent.io/platform/current/connect/references/restapi.html)

---

*This course is part of the comprehensive Kafka Ecosystem Masterclass. Each day is designed to be published as a standalone Medium article while building towards complete Kafka Connect mastery.*
