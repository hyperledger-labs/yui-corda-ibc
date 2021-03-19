ARG GO_VER=1.16.2

FROM golang:1.16.2-buster AS lightclientd

COPY ./external/fabric-ibc-lightclientd /usr/src/fabric-ibc-lightclientd
WORKDIR /usr/src/fabric-ibc-lightclientd

RUN go build

FROM openjdk:8-slim

COPY . /usr/src/app
COPY --from=lightclientd /usr/src/fabric-ibc-lightclientd/fabric-ibc-lightclientd /usr/src/app
WORKDIR /usr/src/app
EXPOSE 9999/tcp

RUN apt-get update && apt-get install -y make openssh-client sshpass
RUN make build deployNodes upNodes prepareHostA downNodes

CMD ./fabric-ibc-lightclientd -port 60001 & make upNodes runServerA
