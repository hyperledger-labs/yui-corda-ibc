ARG GO_VER=1.18.5

ARG CLIENT_NAME
ARG DOCKER_REG
ARG DOCKER_REPO
ARG DOCKER_TAG

FROM golang:$GO_VER-buster AS lightclientd
WORKDIR /usr/src/app
COPY . .
WORKDIR /usr/src/app/go
RUN go build -buildvcs=false ./cmd/fabric-ibc-lightclientd

FROM ${DOCKER_REG}${DOCKER_REPO}${CLIENT_NAME}${DOCKER_TAG} AS client

FROM openjdk:8-slim
ARG CLIENT_NAME
WORKDIR /usr/src/app
COPY . .
COPY --from=client /usr/local/bin/$CLIENT_NAME /usr/local/bin
COPY --from=lightclientd /usr/src/app/go/fabric-ibc-lightclientd /usr/local/bin
RUN apt-get update && apt-get install -y libc6 make openssh-client sshpass netcat
RUN make CLIENT=/usr/local/bin/$CLIENT_NAME clean build deployNodes upNodes prepareHost downNodes

EXPOSE 9999/tcp
EXPOSE 29999/tcp
CMD fabric-ibc-lightclientd -port 60001 & make upNodes startServerBank runServer
