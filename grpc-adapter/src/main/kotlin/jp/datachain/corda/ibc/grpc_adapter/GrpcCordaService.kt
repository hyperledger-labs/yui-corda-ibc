package jp.datachain.corda.ibc.grpc_adapter

import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.grpc.CordaServiceGrpc
import jp.datachain.corda.ibc.grpc.Parties
import jp.datachain.corda.ibc.grpc.PartiesFromNameRequest
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort

class GrpcCordaService(host: String, port: Int, username: String, password: String): CordaServiceGrpc.CordaServiceImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun partiesFromName(request: PartiesFromNameRequest, responseObserver: StreamObserver<Parties>) {
        val parties = ops.partiesFromName(request.name, request.exactMatch)
        val response = Parties.newBuilder()
                .addAllParties(parties.map{it.into()})
                .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}