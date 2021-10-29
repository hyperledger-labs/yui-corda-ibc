ARG RUST_VER=1.54
ARG CLIENT_NAME=yui-corda-ibc-client

FROM rust:$RUST_VER-slim as builder
WORKDIR /usr/src/app
COPY . .
RUN apt-get update && apt-get install -y protobuf-compiler make && rustup component add rustfmt
RUN make buildClient

FROM debian:buster-slim
ARG CLIENT_NAME
COPY --from=builder /usr/src/app/rust/target/debug/$CLIENT_NAME /usr/local/bin
ENV CLIENT_NAME $CLIENT_NAME
CMD $CLIENT_NAME
