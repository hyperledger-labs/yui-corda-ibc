package relay

import (
	"context"

	"github.com/cosmos/cosmos-sdk/codec/types"
	sdk "github.com/cosmos/cosmos-sdk/types"
	clienttypes "github.com/cosmos/ibc-go/v4/modules/core/02-client/types"
	cordatypes "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-corda/types"
	"github.com/hyperledger-labs/yui-relayer/core"
)

// CreateMsgCreateClient creates a CreateClientMsg to this chain
func (pr *Prover) CreateMsgCreateClient(clientID string, dstHeader core.HeaderI, signer sdk.AccAddress) (*clienttypes.MsgCreateClient, error) {
	// information for building client/consensus state can be obtained from host state
	res, err := pr.chain.client.host.QueryHost(
		context.TODO(),
		&cordatypes.QueryHostRequest{},
	)
	if err != nil {
		return nil, err
	}
	clientState := cordatypes.ClientState{
		BaseId:    res.Host.BaseId,
		NotaryKey: res.Host.Notary.OwningKey,
	}
	consensusState := cordatypes.ConsensusState{}

	if anyClientState, err := types.NewAnyWithValue(&clientState); err != nil {
		return nil, err
	} else if anyConsensusState, err := types.NewAnyWithValue(&consensusState); err != nil {
		return nil, err
	} else {
		return &clienttypes.MsgCreateClient{
			ClientState:    anyClientState,
			ConsensusState: anyConsensusState,
			Signer:         signer.String(),
		}, nil
	}
}
