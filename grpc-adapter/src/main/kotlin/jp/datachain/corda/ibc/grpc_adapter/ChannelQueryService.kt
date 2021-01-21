package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.channel.v1.QueryGrpc
import ibc.core.channel.v1.QueryOuterClass
import ibc.core.client.v1.Client
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class ChannelQueryService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): QueryGrpc.QueryImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun channel(request: QueryOuterClass.QueryChannelRequest, responseObserver: StreamObserver<QueryOuterClass.QueryChannelResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryChannelResponse.newBuilder()
                .setChannel(stateAndRef.state.data.end)
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetCommitment(request: QueryOuterClass.QueryPacketCommitmentRequest, responseObserver: StreamObserver<QueryOuterClass.QueryPacketCommitmentResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryPacketCommitmentResponse.newBuilder()
                .setCommitment(stateAndRef.state.data.packets[request.sequence]!!.toByteString())
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetReceipt(request: QueryOuterClass.QueryPacketReceiptRequest, responseObserver: StreamObserver<QueryOuterClass.QueryPacketReceiptResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryPacketReceiptResponse.newBuilder()
                .setReceived(stateAndRef.state.data.receipts.contains(request.sequence))
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetAcknowledgement(request: QueryOuterClass.QueryPacketAcknowledgementRequest, responseObserver: StreamObserver<QueryOuterClass.QueryPacketAcknowledgementResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val reply = QueryOuterClass.QueryPacketAcknowledgementResponse.newBuilder()
                .setAcknowledgement(stateAndRef.state.data.acknowledgements[request.sequence]!!.toByteString())
                .setProof(proof.toByteString())
                .setProofHeight(Client.Height.getDefaultInstance())
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}