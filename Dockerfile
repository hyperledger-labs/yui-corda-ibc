ARG GO_VER=1.16.2

FROM golang:1.16.2-buster AS lightclientd
WORKDIR /usr/src/app
COPY ./external/fabric-ibc-lightclientd .
RUN go build

FROM openjdk:8-slim
WORKDIR /usr/src/app
COPY . .
COPY --from=ghcr.io/datachainlab/corda-ibc-client /usr/local/bin/corda-ibc-client /usr/local/bin
COPY --from=lightclientd /usr/src/app/fabric-ibc-lightclientd /usr/local/bin
RUN apt-get update && apt-get install -y libc6 make openssh-client sshpass
RUN make CLIENT=/usr/local/bin/corda-ibc-client build deployNodes upNodes prepareHostA downNodes

EXPOSE 9999/tcp
CMD fabric-ibc-lightclientd -port 60001 & make upNodes runServerA
