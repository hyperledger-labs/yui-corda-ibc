package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.Node
import ibc.lightclients.corda.v1.NodeServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics20.Address

class NodeService(host: String, port: Int, username: String, password: String): NodeServiceGrpc.NodeServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun partyFromName(request: Node.PartyFromNameRequest, responseObserver: StreamObserver<Node.PartyFromNameResponse>) {
        val party = ops.partiesFromName(request.name, request.exactMatch).single()
        val response = Node.PartyFromNameResponse.newBuilder()
                .setParty(party.into())
                .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun addressFromName(request: Node.AddressFromNameRequest, responseObserver: StreamObserver<Node.AddressFromNameResponse>) {
        val party = ops.partiesFromName(request.name, request.exactMatch).single()
        val address = Address.fromPublicKey(party.owningKey).toBech32()
        val response = Node.AddressFromNameResponse.newBuilder()
                .setAddress(address)
                .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}