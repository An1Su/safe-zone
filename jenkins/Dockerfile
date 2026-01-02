# Custom Jenkins image with Maven, Node.js, and Docker CLI
# Based on official Jenkins LTS with Java 17

FROM jenkins/jenkins:lts-jdk17

# Switch to root to install packages
USER root

# Install essential tools and Docker CLI
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    docker.io \
    && rm -rf /var/lib/apt/lists/*

# Install Maven 3.9.x
ARG MAVEN_VERSION=3.9.6
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xzf - -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

# Install Node.js 20.x LTS
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install Docker Compose standalone
RUN curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-aarch64" -o /usr/local/bin/docker-compose \
    && chmod +x /usr/local/bin/docker-compose

# Add jenkins user to docker group
RUN usermod -aG docker jenkins

# Set environment variables
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# Switch back to jenkins user
USER jenkins
