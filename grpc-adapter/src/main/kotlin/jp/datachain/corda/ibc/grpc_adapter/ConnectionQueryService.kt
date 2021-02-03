package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.connection.v1.Connection
import ibc.core.connection.v1.QueryGrpc
import ibc.core.connection.v1.QueryOuterClass
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.clients.corda.toSignedTransaction
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class ConnectionQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryGrpc.QueryImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun connection(request: QueryOuterClass.QueryConnectionRequest, responseObserver: StreamObserver<QueryOuterClass.QueryConnectionResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcConnection>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.connectionId).toUUID())
        )).states.singleOrNull()
        if (stateAndRef != null) {
            val stx = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!
            stx.verifyRequiredSignatures()
            val proof = stx.toProof()
            assert(proof.toSignedTransaction().tx.outputsOfType<IbcConnection>().single() == stateAndRef.state.data)
            val reply = QueryOuterClass.QueryConnectionResponse.newBuilder()
                    .setConnection(stateAndRef.state.data.end)
                    .setProof(proof.toByteString())
                    .setProofHeight(HEIGHT)
                    .build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        } else {
            responseObserver.onNext(QueryOuterClass.QueryConnectionResponse.newBuilder()
                    .setConnection(Connection.ConnectionEnd.getDefaultInstance())
                    .build())
            responseObserver.onCompleted()
        }
    }
}