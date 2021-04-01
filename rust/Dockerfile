ARG RUST_VER=1.51

FROM rust:${RUST_VER}-slim as builder
ARG APP_NAME=corda-ibc-client
WORKDIR /usr/src/corda-ibc
COPY . .
WORKDIR /usr/src/corda-ibc/rust
RUN apt-get update && apt-get install -y protobuf-compiler && rustup component add rustfmt
RUN cargo clean
RUN cargo build --release

FROM debian:buster-slim
ARG APP_NAME=corda-ibc-client
COPY --from=builder /usr/src/corda-ibc/rust/target/release/$APP_NAME /usr/local/bin
ENV APP_NAME $APP_NAME
CMD $APP_NAME
