package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.Client
import ibc.core.connection.v1.QueryGrpc
import ibc.core.connection.v1.QueryOuterClass
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.clients.corda.toSignedTransaction
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort

class ConnectionQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryGrpc.QueryImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun connection(request: QueryOuterClass.QueryConnectionRequest, responseObserver: StreamObserver<QueryOuterClass.QueryConnectionResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcConnection>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.connectionId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        assert(proof.toSignedTransaction().tx.outputsOfType<IbcConnection>().single() == stateAndRef.state.data)
        val reply = QueryOuterClass.QueryConnectionResponse.newBuilder()
                .setConnection(stateAndRef.state.data.end)
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}