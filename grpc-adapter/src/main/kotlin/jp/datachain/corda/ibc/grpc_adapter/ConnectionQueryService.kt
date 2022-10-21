package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.connection.v1.Connection
import ibc.core.connection.v1.QueryOuterClass
import ibc.lightclients.corda.v1.ConnectionQueryGrpc
import ibc.lightclients.corda.v1.QueryConnection
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.clients.corda.toSignedTransaction
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class ConnectionQueryService(host: String, port: Int, username: String, password: String): ConnectionQueryGrpc.ConnectionQueryImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun connection(request: QueryConnection.QueryConnectionRequest, responseObserver: StreamObserver<QueryConnection.QueryConnectionResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcConnection>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.connectionId).toUUID())
        )).states.singleOrNull()
        if (stateAndRef != null) {
            val stx = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!
            stx.verifyRequiredSignatures()
            val proof = stx.toProof()
            assert(proof.toSignedTransaction().tx.outputsOfType<IbcConnection>().single() == stateAndRef.state.data)
            val response = QueryOuterClass.QueryConnectionResponse.newBuilder()
                    .setConnection(stateAndRef.state.data.end)
                    .setProof(proof.toByteString())
                    .setProofHeight(HEIGHT)
                    .build()
            val reply = QueryConnection.QueryConnectionResponse.newBuilder()
                    .setResponse(response)
                    .build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        } else {
            val response = QueryOuterClass.QueryConnectionResponse.newBuilder()
                    .setConnection(Connection.ConnectionEnd.getDefaultInstance())
                    .build()
            val reply = QueryConnection.QueryConnectionResponse.newBuilder()
                    .setResponse(response)
                    .build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        }
    }
}