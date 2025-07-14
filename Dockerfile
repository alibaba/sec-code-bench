FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    software-properties-common \
    && rm -rf /var/lib/apt/lists/*

# install Python 3.12
RUN add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get install -y python3.12 python3.12-venv python3.12-dev python3-pip && \
    update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.12 1 && \
    update-alternatives --install /usr/bin/python python /usr/bin/python3.12 1 && \
    rm -rf /var/lib/apt/lists/*

# install Java JDK 17
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk && \
    rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# install Maven 3.9.6
RUN wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
    tar -xzf apache-maven-3.9.6-bin.tar.gz && \
    mv apache-maven-3.9.6 /opt/maven && \
    rm apache-maven-3.9.6-bin.tar.gz
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

# install uv
RUN curl -LsSf https://astral.sh/uv/install.sh | sh
ENV PATH=/root/.local/bin:$PATH

WORKDIR /sec-code-bench

COPY . /sec-code-bench/

# install project dependencies
RUN uv self update && uv sync

CMD ["/bin/bash"] 