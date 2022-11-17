package types

import "github.com/cosmos/ibc-go/v4/modules/core/exported"

var _ exported.ConsensusState = (*ConsensusState)(nil)

func (*ConsensusState) ClientType() string {
	return CordaClientType
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
