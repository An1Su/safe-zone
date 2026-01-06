FROM jenkins/jenkins:lts
USER root
RUN apt-get update && \
    apt-get install -y docker.io docker-compose-plugin curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
USER jenkins
