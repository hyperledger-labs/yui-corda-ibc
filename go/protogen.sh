#!/usr/bin/env bash

set -eo pipefail

ibc_go_root='../external/ibc-go'
fabric_ibc_root='../external/yui-fabric-ibc'

proto_root='../proto/src/main/proto'
proto_dirs="$proto_root/ibc/lightclients/corda/v1"
proto_dirs+=" $proto_root/ibc/lightclientd/corda/v1"
proto_dirs+=" $proto_root/ibc/lightclientd/fabric/v1"
for dir in $proto_dirs; do
  protoc \
  -I "$proto_root" \
  -I "$ibc_go_root/proto" \
  -I "$ibc_go_root/third_party/proto" \
  -I "$fabric_ibc_root/proto" \
  --gocosmos_out=plugins=interfacetype+grpc,\
Mgoogle/protobuf/any.proto=github.com/cosmos/cosmos-sdk/codec/types:. \
  $(find "${dir}" -maxdepth 1 -name '*.proto')

  # command to generate gRPC gateway (*.pb.gw.go in respective modules) files
  protoc \
  -I "$proto_root" \
  -I "$ibc_go_root/proto" \
  -I "$ibc_go_root/third_party/proto" \
  -I "$fabric_ibc_root/proto" \
  --grpc-gateway_out=logtostderr=true:. \
  $(find "${dir}" -maxdepth 1 -name '*.proto')

done

# move proto files to the right places
cp -r github.com/hyperledger-labs/yui-corda-ibc/go/* ./
rm -rf github.com
