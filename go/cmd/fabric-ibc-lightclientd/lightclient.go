package main

import (
	"log"
	"strings"

	ics23 "github.com/confio/ics23/go"
	"github.com/cosmos/cosmos-sdk/codec"
	"github.com/cosmos/cosmos-sdk/store/mem"
	sdk "github.com/cosmos/cosmos-sdk/types"
	"github.com/cosmos/cosmos-sdk/types/module"
	clienttypes "github.com/cosmos/ibc-go/modules/core/02-client/types"
	connectiontypes "github.com/cosmos/ibc-go/modules/core/03-connection/types"
	channeltypes "github.com/cosmos/ibc-go/modules/core/04-channel/types"
	commitmenttypes "github.com/cosmos/ibc-go/modules/core/23-commitment/types"
	host "github.com/cosmos/ibc-go/modules/core/24-host"
	"github.com/cosmos/ibc-go/modules/core/exported"
	corda "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-corda"
	pb "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-fabric/types"
	"github.com/hyperledger-labs/yui-fabric-ibc/example"
	fabrictypes "github.com/hyperledger-labs/yui-fabric-ibc/x/ibc/light-clients/xx-fabric/types"
)

type Lightclient struct {
	ctx   sdk.Context
	cdc   codec.BinaryCodec
	store sdk.KVStore
	id    string
	cs    *fabrictypes.ClientState
}

func init() {
	ms := []module.AppModuleBasic{corda.AppModuleBasic{}}
	for _, m := range example.ModuleBasics {
		ms = append(ms, m)
	}
	example.ModuleBasics = module.NewBasicManager(ms...)
}

func mustParseKeyConsensusState(key []byte) clienttypes.Height {
	words := strings.Split(string(key), "/")
	if len(words) != 2 || words[0] != string(host.KeyConsensusStatePrefix) {
		log.Fatalf("failed to split consensus state key: %v", key)
	}
	return clienttypes.MustParseHeight(words[1])
}

func (lc *Lightclient) State() *pb.State {
	state := &pb.State{
		Id:              lc.id,
		ClientState:     lc.cs,
		ConsensusStates: make(map[uint64]*fabrictypes.ConsensusState),
	}
	it := lc.store.Iterator(nil, nil)
	defer it.Close()
	for it.Valid() {
		height := mustParseKeyConsensusState(it.Key()).GetRevisionHeight()
		consensusState := clienttypes.MustUnmarshalConsensusState(lc.cdc, it.Value()).(*fabrictypes.ConsensusState)
		state.ConsensusStates[height] = consensusState
		it.Next()
	}
	return state
}

func (lc *Lightclient) saveConsensusState(height uint64, consensusState *fabrictypes.ConsensusState) {
	lc.store.Set(
		host.ConsensusStateKey(clienttypes.NewHeight(0, height)),
		clienttypes.MustMarshalConsensusState(lc.cdc, consensusState),
	)
}

func NewLightclient(state *pb.State) *Lightclient {
	lc := &Lightclient{
		ctx:   sdk.Context{},
		cdc:   example.MakeEncodingConfig().Marshaler,
		store: mem.NewStore(),
		id:    state.Id,
		cs:    state.ClientState,
	}

	// save consensus states in store
	for height, consensusState := range state.ConsensusStates {
		lc.saveConsensusState(height, consensusState)
	}

	// create lightclient core
	return lc
}

func (lc *Lightclient) ClientType() string {
	return lc.cs.ClientType()
}

func (lc *Lightclient) GetLatestHeight() clienttypes.Height {
	return lc.cs.GetLatestHeight().(clienttypes.Height)
}

func (lc *Lightclient) Validate() error {
	return lc.cs.Validate()
}

func (lc *Lightclient) GetProofSpecs() []*ics23.ProofSpec {
	return lc.cs.GetProofSpecs()
}

func (lc *Lightclient) Initialize(consState *fabrictypes.ConsensusState) error {
	return lc.cs.Initialize(lc.ctx, lc.cdc, lc.store, consState)
}

func (lc *Lightclient) Status() exported.Status {
	return lc.cs.Status(lc.ctx, lc.store, lc.cdc)
}

func (lc *Lightclient) ExportMetadata() []*clienttypes.GenesisMetadata {
	gm := lc.cs.ExportMetadata(lc.store)
	if gm != nil {
		panic("this function should return nil")
	}
	return nil
}

func (lc *Lightclient) CheckHeaderAndUpdateState(header *fabrictypes.Header) error {
	clientState, consensusState, err := lc.cs.CheckHeaderAndUpdateState(lc.ctx, lc.cdc, lc.store, header)
	if err != nil {
		return err
	}
	lc.cs = clientState.(*fabrictypes.ClientState)
	lc.saveConsensusState(header.ChaincodeHeader.Sequence.Value, consensusState.(*fabrictypes.ConsensusState))
	return nil
}

func (lc *Lightclient) VerifyUpgradeAndUpdateState(
	newClient *fabrictypes.ClientState,
	newConsState *fabrictypes.ConsensusState,
	proofUpgradeClient []byte,
	proofUpgradeConsState []byte,
) error {
	_, _, err := lc.cs.VerifyUpgradeAndUpdateState(lc.ctx, lc.cdc, lc.store, newClient, newConsState, proofUpgradeClient, proofUpgradeConsState)
	if err == nil {
		panic("this function should return an error")
	}
	return err
}

func (lc *Lightclient) ZeroCustomFields() *fabrictypes.ClientState {
	return lc.cs.ZeroCustomFields().(*fabrictypes.ClientState)
}

func (lc *Lightclient) VerifyClientState(
	height clienttypes.Height,
	prefix *commitmenttypes.MerklePrefix,
	counterpartyClientIdentifier string,
	proof []byte,
	clientState exported.ClientState,
) error {
	return lc.cs.VerifyClientState(lc.store, lc.cdc, height, prefix, counterpartyClientIdentifier, proof, clientState)
}

func (lc *Lightclient) VerifyClientConsensusState(
	height clienttypes.Height,
	counterpartyClientIdentifier string,
	consensusHeight clienttypes.Height,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	consensusState exported.ConsensusState,
) error {
	return lc.cs.VerifyClientConsensusState(lc.store, lc.cdc, height, counterpartyClientIdentifier, consensusHeight, prefix, proof, consensusState)
}

func (lc *Lightclient) VerifyConnectionState(
	height clienttypes.Height,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	connectionID string,
	connectionEnd connectiontypes.ConnectionEnd,
) error {
	return lc.cs.VerifyConnectionState(lc.store, lc.cdc, height, prefix, proof, connectionID, connectionEnd)
}

func (lc *Lightclient) VerifyChannelState(
	height clienttypes.Height,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	portID,
	channelID string,
	channel channeltypes.Channel,
) error {
	return lc.cs.VerifyChannelState(lc.store, lc.cdc, height, prefix, proof, portID, channelID, channel)
}

func (lc *Lightclient) VerifyPacketCommitment(
	height clienttypes.Height,
	delayTimePeriod uint64,
	delayBlockPeriod uint64,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	portID,
	channelID string,
	sequence uint64,
	commitmentBytes []byte,
) error {
	return lc.cs.VerifyPacketCommitment(lc.ctx, lc.store, lc.cdc, height, delayTimePeriod, delayBlockPeriod, prefix, proof, portID, channelID, sequence, commitmentBytes)
}

func (lc *Lightclient) VerifyPacketAcknowledgement(
	height clienttypes.Height,
	delayTimePeriod uint64,
	delayBlockPeriod uint64,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	portID,
	channelID string,
	sequence uint64,
	acknowledgement []byte,
) error {
	return lc.cs.VerifyPacketAcknowledgement(lc.ctx, lc.store, lc.cdc, height, delayTimePeriod, delayBlockPeriod, prefix, proof, portID, channelID, sequence, acknowledgement)
}

func (lc *Lightclient) VerifyPacketReceiptAbsence(
	height clienttypes.Height,
	delayTimePeriod uint64,
	delayBlockPeriod uint64,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	portID,
	channelID string,
	sequence uint64,
) error {
	return lc.cs.VerifyPacketReceiptAbsence(lc.ctx, lc.store, lc.cdc, height, delayTimePeriod, delayBlockPeriod, prefix, proof, portID, channelID, sequence)
}

func (lc *Lightclient) VerifyNextSequenceRecv(
	height clienttypes.Height,
	delayTimePeriod uint64,
	delayBlockPeriod uint64,
	prefix *commitmenttypes.MerklePrefix,
	proof []byte,
	portID,
	channelID string,
	nextSequenceRecv uint64,
) error {
	return lc.cs.VerifyNextSequenceRecv(lc.ctx, lc.store, lc.cdc, height, delayTimePeriod, delayBlockPeriod, prefix, proof, portID, channelID, nextSequenceRecv)
}
