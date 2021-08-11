package main

import (
	"context"

	"github.com/cosmos/ibc-go/modules/core/exported"
	"github.com/golang/protobuf/ptypes/empty"
	pb "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-fabric/types"
	"google.golang.org/protobuf/types/known/emptypb"
)

type server struct{}

var _ pb.LightClientServer = (*server)(nil)

func (server) ClientType(_ context.Context, req *pb.ClientTypeRequest) (*pb.ClientTypeResponse, error) {
	lc := NewLightclient(req.State)
	return &pb.ClientTypeResponse{ClientType: lc.ClientType()}, nil
}

func (server) GetLatestHeight(_ context.Context, req *pb.GetLatestHeightRequest) (*pb.GetLatestHeightResponse, error) {
	lc := NewLightclient(req.State)
	height := lc.GetLatestHeight()
	return &pb.GetLatestHeightResponse{Height: &height}, nil
}

func (server) Validate(_ context.Context, req *pb.ValidateRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &empty.Empty{}, lc.Validate()
}

func (server) GetProofSpecs(_ context.Context, req *pb.GetProofSpecsRequest) (*pb.GetProofSpecsResponse, error) {
	lc := NewLightclient(req.State)
	return &pb.GetProofSpecsResponse{ProofSpecs: lc.GetProofSpecs()}, nil
}

func (server) Initialize(_ context.Context, req *pb.InitializeRequest) (*pb.InitializeResponse, error) {
	lc := NewLightclient(req.State)
	if err := lc.Initialize(req.ConsensusState); err != nil {
		return nil, err
	}
	return &pb.InitializeResponse{State: lc.State()}, nil
}

func (server) Status(_ context.Context, req *pb.StatusRequest) (*pb.StatusResponse, error) {
	lc := NewLightclient(req.State)
	return &pb.StatusResponse{Status: lc.Status().String()}, nil
}

func (server) ExportMetadata(_ context.Context, req *pb.ExportMetadataRequest) (*pb.ExportMetadataResponse, error) {
	lc := NewLightclient(req.State)
	return &pb.ExportMetadataResponse{GenesisMetadatas: lc.ExportMetadata()}, nil
}

func (server) CheckHeaderAndUpdateState(_ context.Context, req *pb.CheckHeaderAndUpdateStateRequest) (*pb.CheckHeaderAndUpdateStateResponse, error) {
	lc := NewLightclient(req.State)
	if err := lc.CheckHeaderAndUpdateState(req.Header); err != nil {
		return nil, err
	}
	return &pb.CheckHeaderAndUpdateStateResponse{State: lc.State()}, nil
}

func (server) VerifyUpgradeAndUpdateState(_ context.Context, req *pb.VerifyUpgradeAndUpdateStateRequest) (*pb.VerifyUpgradeAndUpdateStateResponse, error) {
	lc := NewLightclient(req.State)
	if err := lc.VerifyUpgradeAndUpdateState(req.NewClient, req.NewConsState, req.ProofUpgradeClient, req.ProofUpgradeConsState); err != nil {
		return nil, err
	}
	return &pb.VerifyUpgradeAndUpdateStateResponse{State: lc.State()}, nil
}

func (server) ZeroCustomFields(_ context.Context, req *pb.ZeroCustomFieldsRequest) (*pb.ZeroCustomFieldsResponse, error) {
	lc := NewLightclient(req.State)
	return &pb.ZeroCustomFieldsResponse{ClientState: lc.ZeroCustomFields()}, nil
}

func (server) VerifyClientState(_ context.Context, req *pb.VerifyClientStateRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	var clientState exported.ClientState
	if err := lc.cdc.UnpackAny(req.ClientState, &clientState); err != nil {
		return nil, err
	}
	return &empty.Empty{}, lc.VerifyClientState(*req.Height, req.Prefix, req.CounterpartyClientIdentifier, req.Proof, clientState)
}

func (server) VerifyClientConsensusState(_ context.Context, req *pb.VerifyClientConsensusStateRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	var consensusState exported.ConsensusState
	if err := lc.cdc.UnpackAny(req.ConsensusState, &consensusState); err != nil {
		return nil, err
	}
	return &empty.Empty{}, lc.VerifyClientConsensusState(*req.Height, req.CounterpartyClientIdentifier, *req.ConsensusHeight, req.Prefix, req.Proof, consensusState)
}

func (server) VerifyConnectionState(_ context.Context, req *pb.VerifyConnectionStateRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &emptypb.Empty{}, lc.VerifyConnectionState(*req.Height, req.Prefix, req.Proof, req.ConnectionId, *req.ConnectionEnd)
}

func (server) VerifyChannelState(_ context.Context, req *pb.VerifyChannelStateRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &empty.Empty{}, lc.VerifyChannelState(*req.Height, req.Prefix, req.Proof, req.PortId, req.ChannelId, *req.Channel)
}

func (server) VerifyPacketCommitment(_ context.Context, req *pb.VerifyPacketCommitmentRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &empty.Empty{}, lc.VerifyPacketCommitment(*req.Height, req.DelayTimePeriod, req.DelayBlockPeriod, req.Prefix, req.Proof, req.PortId, req.ChannelId, req.Sequence, req.CommitmentBytes)
}

func (server) VerifyPacketAcknowledgement(_ context.Context, req *pb.VerifyPacketAcknowledgementRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &empty.Empty{}, lc.VerifyPacketAcknowledgement(*req.Height, req.DelayTimePeriod, req.DelayBlockPeriod, req.Prefix, req.Proof, req.PortId, req.ChannelId, req.Sequence, req.Acknowledgement)
}

func (server) VerifyPacketReceiptAbsence(_ context.Context, req *pb.VerifyPacketReceiptAbsenceRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &empty.Empty{}, lc.VerifyPacketReceiptAbsence(*req.Height, req.DelayTimePeriod, req.DelayBlockPeriod, req.Prefix, req.Proof, req.PortId, req.ChannelId, req.Sequence)
}

func (server) VerifyNextSequenceRecv(_ context.Context, req *pb.VerifyNextSequenceRecvRequest) (*emptypb.Empty, error) {
	lc := NewLightclient(req.State)
	return &empty.Empty{}, lc.VerifyNextSequenceRecv(*req.Height, req.DelayTimePeriod, req.DelayBlockPeriod, req.Prefix, req.Proof, req.PortId, req.ChannelId, req.NextSequenceRecv)
}
