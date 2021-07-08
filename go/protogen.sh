#!/usr/bin/env bash

set -eo pipefail

cosmos_root='../external/ibc-go'

proto_root='../proto/src/main/proto'
proto_dirs="$proto_root/ibc/lightclients/corda/v1"
proto_dirs+=" $proto_root/ibc/lightclientd/corda/v1"
for dir in $proto_dirs; do
  protoc \
  -I "$proto_root" \
  -I "$cosmos_root/proto" \
  -I "$cosmos_root/third_party/proto" \
  --gocosmos_out=plugins=interfacetype+grpc,\
Mgoogle/protobuf/any.proto=github.com/cosmos/cosmos-sdk/codec/types:. \
  $(find "${dir}" -maxdepth 1 -name '*.proto')

  # command to generate gRPC gateway (*.pb.gw.go in respective modules) files
  protoc \
  -I "$proto_root" \
  -I "$cosmos_root/proto" \
  -I "$cosmos_root/third_party/proto" \
  --grpc-gateway_out=logtostderr=true:. \
  $(find "${dir}" -maxdepth 1 -name '*.proto')

done

# move proto files to the right places
cp -r github.com/hyperledger-labs/yui-corda-ibc/go/* ./
rm -rf github.com
