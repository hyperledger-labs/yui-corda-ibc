package relay

import (
	"context"
	"errors"
	"strconv"

	sdk "github.com/cosmos/cosmos-sdk/types"
	"github.com/cosmos/cosmos-sdk/types/query"
	transfertypes "github.com/cosmos/ibc-go/v4/modules/apps/transfer/types"
	clienttypes "github.com/cosmos/ibc-go/v4/modules/core/02-client/types"
	conntypes "github.com/cosmos/ibc-go/v4/modules/core/03-connection/types"
	chantypes "github.com/cosmos/ibc-go/v4/modules/core/04-channel/types"
	ibcexported "github.com/cosmos/ibc-go/v4/modules/core/exported"
	cordatypes "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-corda/types"
)

// QueryLatestHeight queries the chain for the latest height and returns it
func (c *Chain) GetLatestHeight() (int64, error) {
	return 0, nil
}

// QueryClientConsensusState retrevies the latest consensus state for a client in state at a given height
func (c *Chain) QueryClientConsensusState(height int64, dstClientConsHeight ibcexported.Height) (*clienttypes.QueryConsensusStateResponse, error) {
	res, err := c.client.clientQuery.ConsensusState(
		context.TODO(),
		&cordatypes.QueryConsensusStateRequest{
			Request: &clienttypes.QueryConsensusStateRequest{
				ClientId:       c.pathEnd.ClientID,
				RevisionNumber: dstClientConsHeight.GetRevisionNumber(),
				RevisionHeight: dstClientConsHeight.GetRevisionHeight(),
				LatestHeight:   false,
			},
		},
	)
	return res.Response, err
}

// height represents the height of src chain
func (c *Chain) QueryClientState(height int64) (*clienttypes.QueryClientStateResponse, error) {
	res, err := c.client.clientQuery.ClientState(
		context.TODO(),
		&cordatypes.QueryClientStateRequest{
			Request: &clienttypes.QueryClientStateRequest{
				ClientId: c.pathEnd.ClientID,
			},
		},
	)
	return res.Response, err
}

// QueryConnection returns the remote end of a given connection
func (c *Chain) QueryConnection(height int64) (*conntypes.QueryConnectionResponse, error) {
	res, err := c.client.connQuery.Connection(
		context.TODO(),
		&cordatypes.QueryConnectionRequest{
			Request: &conntypes.QueryConnectionRequest{
				ConnectionId: c.pathEnd.ConnectionID,
			},
		},
	)
	return res.Response, err
}

// QueryChannel returns the channel associated with a channelID
func (c *Chain) QueryChannel(height int64) (chanRes *chantypes.QueryChannelResponse, err error) {
	res, err := c.client.chanQuery.Channel(
		context.TODO(),
		&cordatypes.QueryChannelRequest{
			Request: &chantypes.QueryChannelRequest{
				PortId:    c.pathEnd.PortID,
				ChannelId: c.pathEnd.ChannelID,
			},
		},
	)
	return res.Response, err
}

// QueryBalance returns the amount of coins in the relayer account
func (c *Chain) QueryBalance(address sdk.AccAddress) (sdk.Coins, error) {
	addr := address.String()

	res, err := c.client.bank.QueryBank(
		context.TODO(),
		&cordatypes.QueryBankRequest{},
	)
	if err != nil {
		return nil, err
	}

	var coins sdk.Coins
	for denom, allocated := range res.Bank.Allocated.DenomToMap {
		if amount, ok := allocated.PubkeyToAmount[addr]; ok {
			amount, err := strconv.Atoi(amount)
			if err != nil {
				return nil, err
			}
			coins = append(coins, sdk.Coin{
				Denom:  denom,
				Amount: sdk.NewInt(int64(amount)),
			})
		}
	}
	for denom, minted := range res.Bank.Minted.DenomToMap {
		if amount, ok := minted.PubkeyToAmount[addr]; ok {
			amount, err := strconv.Atoi(amount)
			if err != nil {
				return nil, err
			}
			coins = append(coins, sdk.Coin{
				Denom:  denom,
				Amount: sdk.NewInt(int64(amount)),
			})
		}
	}

	return coins, nil
}

// QueryDenomTraces returns all the denom traces from a given chain
func (c *Chain) QueryDenomTraces(offset, limit uint64, height int64) (*transfertypes.QueryDenomTracesResponse, error) {
	return nil, errors.New("QueryDenomTraces is not implemented")
}

// QueryPacketCommitment returns the packet commitment proof at a given height
func (c *Chain) QueryPacketCommitment(height int64, seq uint64) (comRes *chantypes.QueryPacketCommitmentResponse, err error) {
	res, err := c.client.chanQuery.PacketCommitment(
		context.TODO(),
		&cordatypes.QueryPacketCommitmentRequest{
			Request: &chantypes.QueryPacketCommitmentRequest{
				PortId:    c.pathEnd.PortID,
				ChannelId: c.pathEnd.ChannelID,
				Sequence:  seq,
			},
		},
	)
	return res.Response, err
}

// QueryPacketCommitments returns an array of packet commitments
func (c *Chain) QueryPacketCommitments(offset, limit uint64, height int64) (comRes *chantypes.QueryPacketCommitmentsResponse, err error) {
	res, err := c.client.chanQuery.PacketCommitments(
		context.TODO(),
		&cordatypes.QueryPacketCommitmentsRequest{
			Request: &chantypes.QueryPacketCommitmentsRequest{
				PortId:     c.pathEnd.PortID,
				ChannelId:  c.pathEnd.ChannelID,
				Pagination: makePagination(offset, limit),
			},
		},
	)
	return res.Response, err
}

// QueryUnrecievedPackets returns a list of unrelayed packet commitments
func (c *Chain) QueryUnrecievedPackets(height int64, seqs []uint64) ([]uint64, error) {
	res, err := c.client.chanQuery.UnreceivedPackets(
		context.TODO(),
		&cordatypes.QueryUnreceivedPacketsRequest{
			Request: &chantypes.QueryUnreceivedPacketsRequest{
				PortId:                    c.pathEnd.PortID,
				ChannelId:                 c.pathEnd.ChannelID,
				PacketCommitmentSequences: seqs,
			},
		},
	)
	if err != nil {
		return nil, err
	}
	return res.Response.Sequences, nil
}

// QueryPacketAcknowledgements returns an array of packet acks
func (c *Chain) QueryPacketAcknowledgementCommitments(offset, limit uint64, height int64) (comRes *chantypes.QueryPacketAcknowledgementsResponse, err error) {
	res, err := c.client.chanQuery.PacketAcknowledgements(
		context.TODO(),
		&cordatypes.QueryPacketAcknowledgementsRequest{
			Request: &chantypes.QueryPacketAcknowledgementsRequest{
				PortId:     c.pathEnd.PortID,
				ChannelId:  c.pathEnd.ChannelID,
				Pagination: makePagination(offset, limit),
			},
		},
	)
	return res.Response, err
}

// QueryUnrecievedAcknowledgements returns a list of unrelayed packet acks
func (c *Chain) QueryUnrecievedAcknowledgements(height int64, seqs []uint64) ([]uint64, error) {
	res, err := c.client.chanQuery.UnreceivedAcks(
		context.TODO(),
		&cordatypes.QueryUnreceivedAcksRequest{
			Request: &chantypes.QueryUnreceivedAcksRequest{
				PortId:             c.pathEnd.PortID,
				ChannelId:          c.pathEnd.ChannelID,
				PacketAckSequences: seqs,
			},
		},
	)
	if err != nil {
		return nil, err
	}
	return res.Response.Sequences, nil
}

// QueryPacketAcknowledgementCommitment returns the packet ack proof at a given height
func (c *Chain) QueryPacketAcknowledgementCommitment(height int64, seq uint64) (ackRes *chantypes.QueryPacketAcknowledgementResponse, err error) {
	res, err := c.client.chanQuery.PacketAcknowledgement(
		context.TODO(),
		&cordatypes.QueryPacketAcknowledgementRequest{
			Request: &chantypes.QueryPacketAcknowledgementRequest{
				PortId:    c.pathEnd.PortID,
				ChannelId: c.pathEnd.ChannelID,
				Sequence:  seq,
			},
		},
	)
	return res.Response, err
}

// QueryPacket returns a packet corresponds to a given sequence
func (c *Chain) QueryPacket(height int64, sequence uint64) (*chantypes.Packet, error) {
	res, err := c.client.chanQuery.PacketCommitment(
		context.TODO(),
		&cordatypes.QueryPacketCommitmentRequest{
			Request: &chantypes.QueryPacketCommitmentRequest{
				PortId:    c.pathEnd.PortID,
				ChannelId: c.pathEnd.ChannelID,
				Sequence:  sequence,
			},
		},
	)
	if err != nil {
		return nil, err
	}

	// In Corda-IBC, packet commitment = marshaled packet :)
	var packet chantypes.Packet
	if err := packet.Unmarshal(res.Response.Commitment); err != nil {
		return nil, err
	}

	return &packet, nil
}

func (c *Chain) QueryPacketAcknowledgement(height int64, sequence uint64) ([]byte, error) {
	res, err := c.client.chanQuery.PacketAcknowledgement(
		context.TODO(),
		&cordatypes.QueryPacketAcknowledgementRequest{
			Request: &chantypes.QueryPacketAcknowledgementRequest{
				PortId:    c.pathEnd.PortID,
				ChannelId: c.pathEnd.ChannelID,
				Sequence:  sequence,
			},
		},
	)
	if err != nil {
		return nil, err
	}
	// In Corda-IBC, ack commitment = marshaled ack
	return res.Response.Acknowledgement, nil
}

func makePagination(offset, limit uint64) *query.PageRequest {
	return &query.PageRequest{
		Key:        []byte(""),
		Offset:     offset,
		Limit:      limit,
		CountTotal: true,
	}
}
