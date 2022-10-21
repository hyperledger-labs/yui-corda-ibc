package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.Client
import ibc.core.client.v1.QueryOuterClass
import ibc.lightclients.corda.v1.ClientQueryGrpc
import ibc.lightclients.corda.v1.QueryClient
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcClientState
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class ClientQueryService(host: String, port: Int, username: String, password: String): ClientQueryGrpc.ClientQueryImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun clientState(request: QueryClient.QueryClientStateRequest, responseObserver: StreamObserver<QueryClient.QueryClientStateResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcClientState>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.clientId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val response = QueryOuterClass.QueryClientStateResponse.newBuilder()
                .setClientState(stateAndRef.state.data.anyClientState)
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        val reply = QueryClient.QueryClientStateResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun consensusState(request: QueryClient.QueryConsensusStateRequest, responseObserver: StreamObserver<QueryClient.QueryConsensusStateResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcClientState>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.clientId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val clientState = stateAndRef.state.data
        val height =
                if (request.request.latestHeight)
                    clientState.impl.getLatestHeight()
                else
                    Client.Height.newBuilder()
                            .setRevisionNumber(request.request.revisionNumber)
                            .setRevisionHeight(request.request.revisionHeight)
                            .build()
        val response = QueryOuterClass.QueryConsensusStateResponse.newBuilder()
                .setConsensusState(clientState.impl.consensusStates[height]!!.anyConsensusState)
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        val reply = QueryClient.QueryConsensusStateResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}