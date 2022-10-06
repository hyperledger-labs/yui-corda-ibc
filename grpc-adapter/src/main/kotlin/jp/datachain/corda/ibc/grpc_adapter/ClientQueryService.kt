package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.Client
import ibc.core.client.v1.QueryGrpc
import ibc.core.client.v1.QueryOuterClass
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcClientState
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class ClientQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryGrpc.QueryImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun clientState(request: QueryOuterClass.QueryClientStateRequest, responseObserver: StreamObserver<QueryOuterClass.QueryClientStateResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcClientState>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.clientId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryClientStateResponse.newBuilder()
                .setClientState(stateAndRef.state.data.anyClientState)
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun consensusState(request: QueryOuterClass.QueryConsensusStateRequest, responseObserver: StreamObserver<QueryOuterClass.QueryConsensusStateResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcClientState>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.clientId).toUUID())
        )).states.single()
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val clientState = stateAndRef.state.data
        val height =
                if (request.latestHeight)
                    clientState.impl.getLatestHeight()
                else
                    Client.Height.newBuilder()
                            .setRevisionNumber(request.revisionNumber)
                            .setRevisionHeight(request.revisionHeight)
                            .build()
        val reply = QueryOuterClass.QueryConsensusStateResponse.newBuilder()
                .setConsensusState(clientState.impl.consensusStates[height]!!.anyConsensusState)
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}