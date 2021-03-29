package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.Node
import ibc.lightclients.corda.v1.NodeServiceGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into

class NodeService(host: String, port: Int, username: String, password: String): NodeServiceGrpc.NodeServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun partiesFromName(request: Node.PartiesFromNameRequest, responseObserver: StreamObserver<Node.PartiesFromNameResponse>) {
        val parties = ops.partiesFromName(request.name, request.exactMatch)
        val response = Node.PartiesFromNameResponse.newBuilder()
                .addAllParties(parties.map{it.into()})
                .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}