package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.Client
import ibc.core.client.v1.QueryGrpc
import ibc.core.client.v1.QueryOuterClass
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort

class ClientQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryGrpc.QueryImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun clientState(request: QueryOuterClass.QueryClientStateRequest, responseObserver: StreamObserver<QueryOuterClass.QueryClientStateResponse>) {
        val stateAndRef = ops.vaultQueryBy<ClientState>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.clientId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryClientStateResponse.newBuilder()
                .setClientState(stateAndRef.state.data.clientState)
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun consensusState(request: QueryOuterClass.QueryConsensusStateRequest, responseObserver: StreamObserver<QueryOuterClass.QueryConsensusStateResponse>) {
        val stateAndRef = ops.vaultQueryBy<ClientState>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.clientId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val clientState = stateAndRef.state.data
        val height =
                if (request.latestHeight)
                    clientState.getLatestHeight()
                else
                    Client.Height.newBuilder()
                            .setVersionNumber(request.versionNumber)
                            .setVersionHeight(request.versionHeight)
                            .build()
        val reply = QueryOuterClass.QueryConsensusStateResponse.newBuilder()
                .setConsensusState(clientState.consensusStates[height]!!.consensusState)
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}