package main

import (
	"flag"
	"fmt"
	"log"
	"net"

	"google.golang.org/grpc"

	pb "github.com/hyperledger-labs/yui-corda-ibc/go/x/ibc/light-clients/xx-fabric/types"
)

var port uint

func init() {
	flag.UintVar(&port, "port", 60000, "port to listen on")
	flag.Parse()
}

func main() {
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterLightClientServer(s, &server{})
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve gRPC: %v", err)
	}
}
