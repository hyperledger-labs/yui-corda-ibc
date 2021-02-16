package jp.datachain.corda.ibc.grpc_adapter

import ibc.lightclients.corda.v1.NodeServiceGrpc
import ibc.lightclients.corda.v1.Operation
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.conversion.into

class CordaNodeService(host: String, port: Int, username: String, password: String): NodeServiceGrpc.NodeServiceImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun partiesFromName(request: Operation.PartiesFromNameRequest, responseObserver: StreamObserver<Operation.PartiesFromNameResponse>) {
        val parties = ops.partiesFromName(request.name, request.exactMatch)
        val response = Operation.PartiesFromNameResponse.newBuilder()
                .addAllParties(parties.map{it.into()})
                .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}