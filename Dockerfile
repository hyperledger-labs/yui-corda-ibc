FROM openjdk:8-slim

ARG PORT

COPY . /usr/src/app
WORKDIR /usr/src/app
EXPOSE 9999/tcp

RUN apt-get update && apt-get install -y make openssh-client sshpass
RUN make build deployNodes upNodes prepareHostA downNodes

CMD make upNodes runServerA
