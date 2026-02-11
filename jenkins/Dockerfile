# Custom Jenkins image with Maven, Node.js, Docker CLI, and Chromium
# Based on official Jenkins LTS with Java 17

FROM jenkins/jenkins:lts-jdk17

# Switch to root to install packages
USER root

# Copy entrypoint script
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

# Install essential tools, Docker CLI, gosu, and Chromium for frontend tests
RUN apt-get update && apt-get install -y \
  curl \
  wget \
  git \
  docker.io \
  gosu \
  chromium \
  chromium-driver \
  && rm -rf /var/lib/apt/lists/*

# Set Chrome environment variables for Karma
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMIUM_BIN=/usr/bin/chromium

# Install Maven 3.9.x
ARG MAVEN_VERSION=3.9.6
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xzf - -C /opt \
  && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
  && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# Install Node.js 20.x LTS
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
  && apt-get install -y nodejs \
  && rm -rf /var/lib/apt/lists/*

# Install Docker Compose standalone (auto-detect architecture)
RUN ARCH=$(uname -m) && \
  if [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then \
    COMPOSE_ARCH="aarch64"; \
  else \
    COMPOSE_ARCH="x86_64"; \
  fi && \
  curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-linux-${COMPOSE_ARCH}" -o /usr/local/bin/docker-compose \
  && chmod +x /usr/local/bin/docker-compose

# Install SonarScanner CLI (Java-based, platform independent)
ARG SONAR_SCANNER_VERSION=5.0.1.3006
RUN apt-get update && apt-get install -y unzip \
  && curl -fsSL https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}.zip -o /tmp/sonar-scanner.zip \
  && unzip /tmp/sonar-scanner.zip -d /opt \
  && ln -s /opt/sonar-scanner-${SONAR_SCANNER_VERSION}/bin/sonar-scanner /usr/local/bin/sonar-scanner \
  && rm /tmp/sonar-scanner.zip \
  && rm -rf /var/lib/apt/lists/*

# Make entrypoint executable
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Create docker group and add jenkins user
RUN groupadd -f docker && usermod -aG docker jenkins

# Set environment variables
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Use custom entrypoint
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

# Stay as root for entrypoint (it will switch to jenkins)
USER root
