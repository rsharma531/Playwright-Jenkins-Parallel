# Local Jenkins controller for practicing this pipeline.
# Extends the official LTS image with the Docker CLI so pipeline stages can
# start sibling containers (the Playwright image) through the host's Docker
# daemon, whose socket docker-compose mounts into this container.
FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
    && apt-get install -y ca-certificates curl \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
       https://download.docker.com/linux/debian $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
       > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y docker-ce-cli \
    && rm -rf /var/lib/apt/lists/*

# Pre-install the plugins this pipeline needs (plus their dependencies).
RUN jenkins-plugin-cli --plugins "workflow-aggregator docker-workflow git junit matrix-project"
