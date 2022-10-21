package jp.datachain.corda.ibc.grpc_adapter

import cosmos.base.query.v1beta1.Pagination
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.channel.v1.QueryOuterClass
import ibc.lightclients.corda.v1.ChannelQueryGrpc
import ibc.lightclients.corda.v1.QueryChannel
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.ics20.toJson
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria

class ChannelQueryService(host: String, port: Int, username: String, password: String): ChannelQueryGrpc.ChannelQueryImplBase(), CordaRPCOpsReady by CordaRPCOpsReady.create(host, port, username, password) {
    override fun channel(request: QueryChannel.QueryChannelRequest, responseObserver: StreamObserver<QueryChannel.QueryChannelResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.singleOrNull()
        if (stateAndRef != null) {
            assert(stateAndRef.state.data.portId.id == request.request.portId)
            val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
            val response = QueryOuterClass.QueryChannelResponse.newBuilder()
                    .setChannel(stateAndRef.state.data.end)
                    .setProof(proof.toByteString())
                    .setProofHeight(HEIGHT)
                    .build()
            val reply = QueryChannel.QueryChannelResponse.newBuilder()
                    .setResponse(response)
                    .build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        } else {
            val response = QueryOuterClass.QueryChannelResponse.newBuilder()
                    .setChannel(ChannelOuterClass.Channel.getDefaultInstance())
                    .build()
            val reply = QueryChannel.QueryChannelResponse.newBuilder()
                    .setResponse(response)
                    .build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        }
    }

    override fun packetCommitment(request: QueryChannel.QueryPacketCommitmentRequest, responseObserver: StreamObserver<QueryChannel.QueryPacketCommitmentResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val response = QueryOuterClass.QueryPacketCommitmentResponse.newBuilder()
                .setCommitment(stateAndRef.state.data.packets[request.request.sequence]!!.toByteString())
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        val reply = QueryChannel.QueryPacketCommitmentResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetCommitments(request: QueryChannel.QueryPacketCommitmentsRequest, responseObserver: StreamObserver<QueryChannel.QueryPacketCommitmentsResponse>) {
        assert(request.request.pagination.key.isEmpty)
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)
        // TODO: the following is very inefficient code ...
        val commitments = stateAndRef.state.data.packets.values
                .sortedBy{it.sequence}
                .filter{it.sequence >= request.request.pagination.offset}
                .take(request.request.pagination.limit.toInt())
                .map {
                    ChannelOuterClass.PacketState.newBuilder()
                            .setPortId(request.request.portId)
                            .setChannelId(request.request.channelId)
                            .setSequence(it.sequence)
                            .setData(it.toByteString())
                            .build()
                }
        val response = QueryOuterClass.QueryPacketCommitmentsResponse.newBuilder()
                .addAllCommitments(commitments)
                .setPagination(Pagination.PageResponse.newBuilder().apply{
                    if (request.request.pagination.countTotal) {
                        total = commitments.size.toLong()
                    }
                })
                .setHeight(HEIGHT)
        val reply = QueryChannel.QueryPacketCommitmentsResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetReceipt(request: QueryChannel.QueryPacketReceiptRequest, responseObserver: StreamObserver<QueryChannel.QueryPacketReceiptResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val response = QueryOuterClass.QueryPacketReceiptResponse.newBuilder()
                .setReceived(stateAndRef.state.data.receipts.contains(request.request.sequence))
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        val reply = QueryChannel.QueryPacketReceiptResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetAcknowledgement(request: QueryChannel.QueryPacketAcknowledgementRequest, responseObserver: StreamObserver<QueryChannel.QueryPacketAcknowledgementResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)
        val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
        val response = QueryOuterClass.QueryPacketAcknowledgementResponse.newBuilder()
                .setAcknowledgement(stateAndRef.state.data.acknowledgements[request.request.sequence]!!.toJson())
                .setProof(proof.toByteString())
                .setProofHeight(HEIGHT)
                .build()
        val reply = QueryChannel.QueryPacketAcknowledgementResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetAcknowledgements(request: QueryChannel.QueryPacketAcknowledgementsRequest, responseObserver: StreamObserver<QueryChannel.QueryPacketAcknowledgementsResponse>) {
        assert(request.request.pagination.key.isEmpty)
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)
        // TODO: the following is very inefficient code ...
        val acks = stateAndRef.state.data.acknowledgements.toSortedMap()
                .subMap(request.request.pagination.offset, Long.MAX_VALUE)
                .toList()
                .take(request.request.pagination.limit.toInt())
                .map {
                    ChannelOuterClass.PacketState.newBuilder()
                            .setPortId(request.request.portId)
                            .setChannelId(request.request.channelId)
                            .setSequence(it.first)
                            .setData(it.second.toJson())
                            .build()
                }
        val response = QueryOuterClass.QueryPacketAcknowledgementsResponse.newBuilder()
                .addAllAcknowledgements(acks)
                .setPagination(Pagination.PageResponse.newBuilder().apply{
                    if (request.request.pagination.countTotal) {
                        total = acks.size.toLong()
                    }
                })
                .setHeight(HEIGHT)
                .build()
        val reply = QueryChannel.QueryPacketAcknowledgementsResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun unreceivedPackets(request: QueryChannel.QueryUnreceivedPacketsRequest, responseObserver: StreamObserver<QueryChannel.QueryUnreceivedPacketsResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)
        val unreceived = request.request.packetCommitmentSequencesList.filter{!stateAndRef.state.data.receipts.contains(it)}
        val response = QueryOuterClass.QueryUnreceivedPacketsResponse.newBuilder()
                .addAllSequences(unreceived)
                .setHeight(HEIGHT)
        val reply = QueryChannel.QueryUnreceivedPacketsResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun unreceivedAcks(request: QueryChannel.QueryUnreceivedAcksRequest, responseObserver: StreamObserver<QueryChannel.QueryUnreceivedAcksResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(request.baseId.into().toString()),
                uuid = listOf(Identifier(request.request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.request.portId)

        // When a packet is sent, it is saved into packets field of channel.
        // When an ack is received, the corresponding packet is removed from this field.
        // Therefore, this field contains only packets that have been sent but not been acknowledged yet.
        val unreceived = request.request.packetAckSequencesList.filter{stateAndRef.state.data.packets.containsKey(it)}

        val response = QueryOuterClass.QueryUnreceivedAcksResponse.newBuilder()
                .addAllSequences(unreceived)
                .setHeight(HEIGHT)
                .build()
        val reply = QueryChannel.QueryUnreceivedAcksResponse.newBuilder()
                .setResponse(response)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}