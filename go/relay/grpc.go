package relay

import (
	cordatypes "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-corda/types"
	"google.golang.org/grpc"
)

type cordaIbcClient struct {
	conn *grpc.ClientConn

	node cordatypes.NodeServiceClient

	host cordatypes.HostServiceClient
	bank cordatypes.BankServiceClient

	clientQuery cordatypes.ClientQueryClient
	connQuery   cordatypes.ConnectionQueryClient
	chanQuery   cordatypes.ChannelQueryClient

	clientTx   cordatypes.ClientMsgClient
	connTx     cordatypes.ConnectionMsgClient
	chanTx     cordatypes.ChannelMsgClient
	transferTx cordatypes.TransferMsgClient
}

func createCordaIbcClient(addr string) (*cordaIbcClient, error) {
	conn, err := grpc.Dial(addr, grpc.WithInsecure(), grpc.WithBlock())
	if err != nil {
		return nil, err
	}

	return &cordaIbcClient{
		conn: conn,

		node: cordatypes.NewNodeServiceClient(conn),

		host: cordatypes.NewHostServiceClient(conn),
		bank: cordatypes.NewBankServiceClient(conn),

		clientQuery: cordatypes.NewClientQueryClient(conn),
		connQuery:   cordatypes.NewConnectionQueryClient(conn),
		chanQuery:   cordatypes.NewChannelQueryClient(conn),

		clientTx:   cordatypes.NewClientMsgClient(conn),
		connTx:     cordatypes.NewConnectionMsgClient(conn),
		chanTx:     cordatypes.NewChannelMsgClient(conn),
		transferTx: cordatypes.NewTransferMsgClient(conn),
	}, nil
}

func (gc *cordaIbcClient) shutdown() error {
	return gc.conn.Close()
}
