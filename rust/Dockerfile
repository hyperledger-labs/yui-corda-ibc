ARG RUST_VER=1.51
ARG ALPINE_VER=3.13

FROM rust:${RUST_VER}-alpine${ALPINE_VER} as builder
ARG APP_NAME=corda-ibc-client
WORKDIR /usr/src/corda-ibc
COPY . .
WORKDIR /usr/src/corda-ibc/rust
RUN apk add --no-cache musl-dev protoc && rustup component add rustfmt
RUN cargo clean
RUN cargo build --release

FROM alpine:$ALPINE_VER
ARG APP_NAME=corda-ibc-client
COPY --from=builder /usr/src/corda-ibc/rust/target/release/$APP_NAME /usr/local/bin
ENV APP_NAME $APP_NAME
CMD $APP_NAME
