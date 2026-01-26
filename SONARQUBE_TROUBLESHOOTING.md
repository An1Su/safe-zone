# SonarQube Troubleshooting Guide

## Quick Diagnosis Steps

### 1. Check if SonarQube Container is Running

```bash
# Check container status
docker ps -a | grep sonarqube

# Check logs
docker logs sonarqube --tail 50

# Start SonarQube if stopped
cd sonarqube
docker compose up -d

# Wait for SonarQube to be ready (2-3 minutes)
sleep 180
```

### 2. Verify SonarQube is Accessible

```bash
# From host machine
curl http://localhost:9000/api/system/status

# Expected response: {"status":"UP","version":"..."}

# Check if SonarQube UI is accessible
open http://localhost:9000
```

### 3. Check Jenkins Connection to SonarQube

```bash
# From Jenkins container
docker exec jenkins curl -s http://host.docker.internal:9000/api/system/status

# If this fails, SonarQube is not accessible from Jenkins
```

### 4. Verify Project Exists in SonarQube

1. Login to SonarQube: http://localhost:9000
2. Check if project `safe-zone` exists
3. If not, create it:
   - Click **"Create Project"** → **"Manually"**
   - Project key: `safe-zone`
   - Display name: `SafeZone E-commerce Platform`

### 5. Verify Token Configuration

**In SonarQube:**
1. Login → **My Account** → **Security** → **Tokens**
2. Check if token `jenkins-safe-zone` exists
3. If not, create new token:
   - Name: `jenkins-safe-zone`
   - Type: **User Token**
   - Expiration: **No expiration** (or set long expiration)
   - **Copy token immediately** (you won't see it again!)

**In Jenkins:**
1. **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
2. Check if credential with ID `sonarqube-token` exists
3. Verify the secret matches your SonarQube token
4. If missing or wrong:
   - Click **"Add Credentials"**
   - **Kind:** Secret text
   - **Secret:** [Paste SonarQube token]
   - **ID:** `sonarqube-token` ⚠️ (must match exactly!)
   - **Description:** SonarQube authentication token

### 6. Check SonarQube Analysis Logs

```bash
# Check SonarQube logs for errors
docker logs sonarqube --tail 100 | grep -i error

# Check if analysis was received
docker logs sonarqube --tail 100 | grep -i "safe-zone"
```

### 7. Manual Test Analysis

```bash
# Test SonarQube analysis manually
cd backend
./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=safe-zone \
  -Dsonar.projectName="safe-zone" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE \
  -Dsonar.java.source=17
```

## Common Issues & Solutions

### Issue 1: "No scan results in SonarQube UI"

**Possible Causes:**
- SonarQube container stopped
- Project doesn't exist in SonarQube
- Analysis failed silently
- Token is invalid or expired

**Solution:**
1. Check container: `docker ps | grep sonarqube`
2. Check project exists: Login to http://localhost:9000
3. Check Jenkins build logs for SonarQube stage errors
4. Verify token is correct in Jenkins credentials

### Issue 2: "Connection refused" from Jenkins

**Possible Causes:**
- SonarQube not running
- Wrong URL in Jenkinsfile (`host.docker.internal:9000`)
- Network isolation between containers

**Solution:**
```bash
# Verify SonarQube is running
docker ps | grep sonarqube

# Test connectivity from Jenkins container
docker exec jenkins curl -v http://host.docker.internal:9000/api/system/status

# If host.docker.internal doesn't work, try:
# - Use host's IP address instead
# - Or connect Jenkins and SonarQube to same Docker network
```

### Issue 3: "Unauthorized" or "Invalid token"

**Possible Causes:**
- Token expired
- Token doesn't exist
- Wrong token in Jenkins credentials

**Solution:**
1. Generate new token in SonarQube
2. Update Jenkins credential with new token
3. Ensure credential ID is exactly `sonarqube-token`

### Issue 4: "Project not found"

**Possible Causes:**
- Project key mismatch (`safe-zone` vs `SafeZone`)
- Project was deleted
- First analysis hasn't completed

**Solution:**
1. Verify project key in SonarQube matches Jenkinsfile (`safe-zone`)
2. Create project manually if missing
3. Wait for first analysis to complete

### Issue 5: "No coverage data"

**Possible Causes:**
- JaCoCo reports not generated
- Wrong path in `sonar.coverage.jacoco.xmlReportPaths`
- Tests not run before analysis

**Solution:**
1. Ensure tests run before SonarQube analysis stage
2. Verify JaCoCo reports exist:
   ```bash
   ls backend/services/*/target/site/jacoco/jacoco.xml
   ```
3. Check paths in Jenkinsfile match actual report locations

## Network Configuration

### Current Setup
- **SonarQube:** Runs on `localhost:9000` (host machine)
- **Jenkins:** Connects via `http://host.docker.internal:9000`
- **Network:** SonarQube uses `sonar-network`, Jenkins uses default bridge

### If `host.docker.internal` Doesn't Work

**Option 1: Use Host IP**
```bash
# Get host IP
HOST_IP=$(ipconfig getifaddr en0)  # macOS
# or
HOST_IP=$(hostname -I | awk '{print $1}')  # Linux

# Update Jenkinsfile:
-Dsonar.host.url=http://${HOST_IP}:9000
```

**Option 2: Connect to Same Network**
```yaml
# In sonarqube/docker-compose.yml, change network:
networks:
  sonar-network:
    external: true
    name: bridge  # Use default bridge network

# In jenkins/docker-compose.yml, ensure same network
```

## Verification Checklist

- [ ] SonarQube container is running (`docker ps | grep sonarqube`)
- [ ] SonarQube UI accessible (http://localhost:9000)
- [ ] Project `safe-zone` exists in SonarQube
- [ ] Token exists and is valid in SonarQube
- [ ] Jenkins credential `sonarqube-token` exists and matches SonarQube token
- [ ] Jenkins can reach SonarQube (`docker exec jenkins curl http://host.docker.internal:9000/api/system/status`)
- [ ] Last Jenkins build completed SonarQube analysis stage
- [ ] JaCoCo reports are generated before analysis

## Quick Fix Commands

```bash
# Restart SonarQube
cd sonarqube && docker compose restart

# Check SonarQube health
curl http://localhost:9000/api/system/status

# View recent SonarQube logs
docker logs sonarqube --tail 100

# Test Jenkins → SonarQube connection
docker exec jenkins curl -s http://host.docker.internal:9000/api/system/status

# Check if project exists (requires authentication)
curl -u YOUR_TOKEN: http://localhost:9000/api/projects/search?projects=safe-zone
```

## Next Steps After Fixing

1. **Trigger a new Jenkins build** to test the fix
2. **Monitor Jenkins console output** for SonarQube stage
3. **Check SonarQube UI** after build completes
4. **Verify Quality Gate** shows results

---

**Last Updated:** January 2026
