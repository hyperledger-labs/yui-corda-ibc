module github.com/datachainlab/corda-ibc/go

go 1.16

require (
	github.com/cosmos/cosmos-sdk v0.40.0-rc3
	github.com/gogo/protobuf v1.3.1
	google.golang.org/grpc v1.33.0
	google.golang.org/protobuf v1.25.0
)

replace (
	github.com/cosmos/cosmos-sdk => github.com/datachainlab/cosmos-sdk v0.34.4-0.20210312160443-1cdc372f98d0
	github.com/gogo/protobuf => github.com/regen-network/protobuf v1.3.2-alpha.regen.4
)
