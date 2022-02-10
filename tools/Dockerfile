FROM ubuntu:20.04

LABEL version="1.0"
LABEL description="Docker to run Robot Framework"

ENV ROBOT_DIRECTORY=/opt/robot-tests
ENV ROBOT_COMMON_DIRECTORY=$ROBOT_DIRECTORY/common
ENV ROBOT_TESTS_DIRECTORY=$ROBOT_DIRECTORY/tests
ENV ROBOT_RESULTS_DIRECTORY=$ROBOT_DIRECTORY/results


# Create directories structure
RUN mkdir $ROBOT_DIRECTORY
RUN mkdir $ROBOT_COMMON_DIRECTORY
RUN mkdir $ROBOT_TESTS_DIRECTORY
RUN mkdir $ROBOT_RESULTS_DIRECTORY


# Create Volumes for docker
VOLUME $ROBOT_DIRECTORY
VOLUME $ROBOT_COMMON_DIRECTORY
VOLUME $ROBOT_TESTS_DIRECTORY
VOLUME $ROBOT_RESULTS_DIRECTORY

WORKDIR $ROBOT_DIRECTORY

ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get install -y --no-install-recommends  \
    software-properties-common \
    build-essential \
    bash \
    bash-completion \
    curl \
    python3 \
    python3-dev \
    python3-pip \
    python3-venv 


RUN add-apt-repository ppa:deadsnakes/ppa
RUN apt-get install -y python3.9 python3.9-venv python3.9-dev


RUN mkdir /opt/venv
RUN python3.9 -m venv /opt/venv
RUN sh 'source /opt/venv/bin/activate && \
    pip install --upgrade pip && \
    pip install --upgrade robotframework'
 