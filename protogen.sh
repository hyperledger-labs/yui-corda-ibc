#!/usr/bin/env bash

set -eo pipefail

protoc_gen_gocosmos() {
  if ! grep "github.com/gogo/protobuf => github.com/regen-network/protobuf" go.mod &>/dev/null ; then
    echo -e "\tPlease run this command from somewhere inside the cosmos-sdk folder."
    return 1
  fi

  go get github.com/regen-network/cosmos-proto/protoc-gen-gocosmos 2>/dev/null
}

protoc_gen_gocosmos

ibc_go_root='./external/ibc-go'
fabric_ibc_root='./external/yui-fabric-ibc'

proto_root='./proto/src/main/proto'
proto_dirs="$proto_root/ibc/lightclients/corda/v1"
proto_dirs="$proto_dirs $proto_root/ibc/lightclientd/corda/v1"
proto_dirs="$proto_dirs $proto_root/ibc/lightclientd/fabric/v1"
proto_dirs="$proto_dirs $proto_root/relayer/chains/corda"
for dir in $proto_dirs; do
  buf protoc \
  -I "$proto_root" \
  -I "$ibc_go_root/proto" \
  -I "$ibc_go_root/third_party/proto" \
  -I "$fabric_ibc_root/proto" \
  --gocosmos_out=plugins=interfacetype+grpc,\
Mgoogle/protobuf/any.proto=github.com/cosmos/cosmos-sdk/codec/types:. \
  $(find "${dir}" -maxdepth 1 -name '*.proto')

  # command to generate gRPC gateway (*.pb.gw.go in respective modules) files
  buf protoc \
  -I "$proto_root" \
  -I "$ibc_go_root/proto" \
  -I "$ibc_go_root/third_party/proto" \
  -I "$fabric_ibc_root/proto" \
  --grpc-gateway_out=logtostderr=true:. \
  $(find "${dir}" -maxdepth 1 -name '*.proto')

done

# move proto files to the right places
cp -r github.com/hyperledger-labs/yui-corda-ibc/go/* ./go
rm -rf github.com
