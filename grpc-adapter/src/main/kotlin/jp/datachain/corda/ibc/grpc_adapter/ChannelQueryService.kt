package jp.datachain.corda.ibc.grpc_adapter

import cosmos.base.query.v1beta1.Pagination
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.channel.v1.QueryGrpc
import ibc.core.channel.v1.QueryOuterClass
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.clients.corda.HEIGHT
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
        )).states.singleOrNull()
        if (stateAndRef != null) {
            assert(stateAndRef.state.data.portId.id == request.portId)
            val proof = ops.internalFindVerifiedTransaction(stateAndRef.ref.txhash)!!.toProof()
            val reply = QueryOuterClass.QueryChannelResponse.newBuilder()
                    .setChannel(stateAndRef.state.data.end)
                    .setProof(proof.toByteString())
                    .setProofHeight(HEIGHT)
                    .build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        } else {
            responseObserver.onNext(QueryOuterClass.QueryChannelResponse.newBuilder()
                    .setChannel(ChannelOuterClass.Channel.getDefaultInstance())
                    .build())
            responseObserver.onCompleted()
        }
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
                .setProofHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetCommitments(request: QueryOuterClass.QueryPacketCommitmentsRequest, responseObserver: StreamObserver<QueryOuterClass.QueryPacketCommitmentsResponse>) {
        assert(request.pagination.key.isEmpty)
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        // TODO: the following is very inefficient code ...
        val commitments = stateAndRef.state.data.packets.values
                .sortedBy{it.sequence}
                .filter{it.sequence >= request.pagination.offset}
                .take(request.pagination.limit.toInt())
                .map {
                    ChannelOuterClass.PacketState.newBuilder()
                            .setPortId(request.portId)
                            .setChannelId(request.channelId)
                            .setSequence(it.sequence)
                            .setData(it.toByteString())
                            .build()
                }
        val reply = QueryOuterClass.QueryPacketCommitmentsResponse.newBuilder()
                .addAllCommitments(commitments)
                .setPagination(Pagination.PageResponse.newBuilder().apply{
                    if (request.pagination.countTotal) {
                        total = commitments.size.toLong()
                    }
                })
                .setHeight(HEIGHT)
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
                .setProofHeight(HEIGHT)
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
                .setProofHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun packetAcknowledgements(request: QueryOuterClass.QueryPacketAcknowledgementsRequest, responseObserver: StreamObserver<QueryOuterClass.QueryPacketAcknowledgementsResponse>) {
        assert(request.pagination.key.isEmpty)
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        // TODO: the following is very inefficient code ...
        val acks = stateAndRef.state.data.acknowledgements.toSortedMap()
                .subMap(request.pagination.offset, Long.MAX_VALUE)
                .toList()
                .take(request.pagination.limit.toInt())
                .map {
                    ChannelOuterClass.PacketState.newBuilder()
                            .setPortId(request.portId)
                            .setChannelId(request.channelId)
                            .setSequence(it.first)
                            .setData(it.second.toByteString())
                            .build()
                }
        val reply = QueryOuterClass.QueryPacketAcknowledgementsResponse.newBuilder()
                .addAllAcknowledgements(acks)
                .setPagination(Pagination.PageResponse.newBuilder().apply{
                    if (request.pagination.countTotal) {
                        total = acks.size.toLong()
                    }
                })
                .setHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun unreceivedPackets(request: QueryOuterClass.QueryUnreceivedPacketsRequest, responseObserver: StreamObserver<QueryOuterClass.QueryUnreceivedPacketsResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)
        val unreceived = request.packetCommitmentSequencesList.filter{!stateAndRef.state.data.receipts.contains(it)}
        val reply = QueryOuterClass.QueryUnreceivedPacketsResponse.newBuilder()
                .addAllSequences(unreceived)
                .setHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun unreceivedAcks(request: QueryOuterClass.QueryUnreceivedAcksRequest, responseObserver: StreamObserver<QueryOuterClass.QueryUnreceivedAcksResponse>) {
        val stateAndRef = ops.vaultQueryBy<IbcChannel>(QueryCriteria.LinearStateQueryCriteria(
                externalId = listOf(baseId.toString()),
                uuid = listOf(Identifier(request.channelId).toUUID())
        )).states.single()
        assert(stateAndRef.state.data.portId.id == request.portId)

        // When a packet is sent, it is saved into packets field of channel.
        // When an ack is received, the corresponding packet is removed from this field.
        // Therefore this field contains only packets that have been sent but not been acknowledged yet.
        val unreceived = request.packetAckSequencesList.filter{stateAndRef.state.data.packets.containsKey(it)}

        val reply = QueryOuterClass.QueryUnreceivedAcksResponse.newBuilder()
                .addAllSequences(unreceived)
                .setHeight(HEIGHT)
                .build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}