# Jenkins CI/CD Setup

This directory contains the Jenkins configuration for the mr-jenk CI/CD pipeline.

## Quick Start

```bash
# Start Jenkins
cd jenkins
docker-compose up -d

# View logs (look for the initial admin password)
docker logs jenkins

# Access Jenkins
open http://localhost:8090
```

## Initial Setup

1. Get the initial admin password from the logs:
   ```bash
   docker logs jenkins 2>&1 | grep -A 2 "initial"
   ```

2. Open http://localhost:8090 in your browser

3. Paste the admin password

4. Install suggested plugins

5. Create your admin user

## Ports

- **8090**: Jenkins Web UI
- **50000**: Jenkins Agent (for distributed builds)

## Data Persistence

Jenkins data is stored in a Docker volume `mr-jenk-jenkins-data`.

To reset Jenkins completely:
```bash
docker-compose down -v
```

## Stop Jenkins

```bash
docker-compose down
```

