# MySQL with Docker Compose

This project runs a **MySQL database** inside a Docker container using **Docker Compose**.
It is intended for local development and testing.

You do **not** need MySQL installed on your machine. Docker handles everything.

---

## What This Setup Does

* Runs **MySQL (official image)** in a container
* Creates a database automatically
* Creates a non-root user automatically
* Stores database data persistently (data survives restarts)
* Exposes MySQL so you can connect from your computer or other containers

---

## Prerequisites

Before starting, make sure you have:

1. **Docker** installed
   [https://www.docker.com/get-started](https://www.docker.com/get-started)

2. **Docker Compose**

    * Included automatically with Docker Desktop (Windows / macOS)
    * On Linux, you may need to install it separately

To verify installation:

```bash
docker --version
docker compose version
```

---

## Files in This Project

```
.
├── docker-compose.yml
└── README.md
```

---

## Docker Compose Configuration Explained

### Service: MySQL

```yaml
services:
  mysql:
    image: mysql:latest
```

* Uses the **official MySQL image** from Docker Hub
* `latest` pulls the most recent stable version

---

### Container Name

```yaml
container_name: mysql
```

* Names the container explicitly as `mysql`
* Makes it easier to reference in commands

---

### Restart Policy

```yaml
restart: unless-stopped
```

* Automatically restarts MySQL if it crashes or Docker restarts
* Stops only if you explicitly stop it

---

### Port Mapping

```yaml
ports:
  - "3306:3306"
```

* Exposes MySQL to your local machine
* You can connect using:

    * Host: `localhost`
    * Port: `3306`

---

### Environment Variables (Database Setup)

```yaml
environment:
  MYSQL_ROOT_PASSWORD: root
  MYSQL_USER: codefarm
  MYSQL_PASSWORD: codefarm
  MYSQL_DATABASE: events
```

On first startup, MySQL will:

* Set the **root password** to `root`
* Create a database named `events`
* Create a user:

    * Username: `codefarm`
    * Password: `codefarm`
* Grant this user access to the `events` database

Important:
These values are applied **only the first time** the database is created.

---

### Persistent Storage (Volumes)

```yaml
volumes:
  - mysql-data:/var/lib/mysql
```

* MySQL stores all data in `/var/lib/mysql`
* `mysql-data` is a Docker-managed volume
* Data persists even if the container is stopped or removed

---

### Network

```yaml
networks:
  - mysql-network
```

* Creates an isolated Docker network for MySQL
* Other containers on this network can connect using the hostname `mysql`

---

## How to Start the Database

From the directory containing `docker-compose.yml`:

```bash
docker compose up -d
```

* `-d` runs containers in the background
* MySQL may take 10–20 seconds to initialize the first time

---

## Check That MySQL Is Running

### List running containers

```bash
docker ps
```

You should see a container named `mysql`.

---

### View MySQL logs

```bash
docker logs mysql
```

Look for a line similar to:

```
MySQL init process done. Ready for start up.
```

---

## Connect to MySQL

### Option 1: From Your Local Machine (CLI)

If you have the MySQL client installed:

```bash
mysql -h localhost -P 3306 -u codefarm -p
```

Enter password when prompted:

```
codefarm
```

To connect as root:

```bash
mysql -h localhost -P 3306 -u root -p
```

Password:

```
root
```

---

### Option 2: Connect From Inside the Container

This works even if MySQL client is not installed locally:

```bash
docker exec -it mysql mysql -u codefarm -p
```

---

## Verify the Database

Once connected to MySQL:

```sql
SHOW DATABASES;
```

You should see:

```
events
```

Switch to the database:

```sql
USE events;
```

---

## Stop the Database

To stop MySQL:

```bash
docker compose down
```

This:

* Stops the container
* Keeps all database data intact

---

## Remove Everything (Including Data)

If you want a **clean reset** (this deletes all data):

```bash
docker compose down -v
```

---

## Common Notes

* The MySQL port is **publicly exposed** on `3306`
* Credentials are **not secure** and intended for local development only
* For production, you should:

    * Use secrets
    * Avoid exposing ports
    * Pin a specific MySQL version

---

## Default Connection Summary

| Setting       | Value       |
| ------------- | ----------- |
| Host          | `localhost` |
| Port          | `3306`      |
| Database      | `events`    |
| User          | `codefarm`  |
| Password      | `codefarm`  |
| Root Password | `root`      |

---
