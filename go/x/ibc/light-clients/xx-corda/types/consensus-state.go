package types

import "github.com/cosmos/cosmos-sdk/x/ibc/core/exported"

var _ exported.ConsensusState = (*ConsensusState)(nil)

func (*ConsensusState) ClientType() string {
	return "corda"
}

// which is used for key-value pair verification.
func (*ConsensusState) GetRoot() exported.Root {
	panic("not implemented")
}

func (*ConsensusState) GetTimestamp() uint64 {
	return 0
}

func (*ConsensusState) ValidateBasic() error {
	return nil
}
