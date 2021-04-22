package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import cosmos.base.query.v1beta1.Pagination
import ibc.applications.transfer.v1.Tx
import ibc.core.client.v1.Client.*
import ibc.core.client.v1.QueryOuterClass.*
import ibc.core.connection.v1.Tx.*
import ibc.core.connection.v1.QueryOuterClass.*
import ibc.core.channel.v1.Tx.*
import ibc.core.channel.v1.QueryOuterClass.*
import ibc.core.channel.v1.ChannelOuterClass.*
import ibc.lightclients.corda.v1.*
import ibc.core.client.v1.MsgGrpc as ClientMsgGrpc
import ibc.core.client.v1.QueryGrpc as ClientQueryGrpc
import ibc.core.connection.v1.MsgGrpc as ConnectionMsgGrpc
import ibc.core.connection.v1.QueryGrpc as ConnectionQueryGrpc
import ibc.core.channel.v1.MsgGrpc as ChannelMsgGrpc
import ibc.core.channel.v1.QueryGrpc as ChannelQueryGrpc
import ibc.applications.transfer.v1.MsgGrpc as TransferMsgGrpc
import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.conversion.into
import io.grpc.StatusRuntimeException
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.utilities.toHex
import java.io.File

object Client {
    @JvmStatic
    fun main(args: Array<String>) {
        when (args[0]) {
            "shutdown" -> shutdown(args[1])
            "createGenesis" -> createGenesis(args[1], args[2], args[3])
            "createHost" -> createHost(args[1])
            "createBank" -> createBank(args[1])
            "allocateFund" -> allocateFund(args[1], args[2])
            "executeTest" -> executeTest(args[1], args[2], args[3], args[4])
        }
    }

    private fun connectGrpc(endpoint: String) = ManagedChannelBuilder.forTarget(endpoint)
            .usePlaintext()
            .build()

    private fun shutdown(endpoint: String) {
        val channel = connectGrpc(endpoint)
        val adminService = AdminServiceGrpc.newBlockingStub(channel)

        try {
            adminService.shutdown(Empty.getDefaultInstance())
        } catch (e: StatusRuntimeException) {
            println(e)
        }
    }

    private fun createGenesis(endpoint: String, partyName: String, baseHashFilePath: String) {
        val channel = connectGrpc(endpoint)
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val genesisService = GenesisServiceGrpc.newBlockingStub(channel)

        val participants = listOf(partyName, "Notary").map {
            nodeService.partiesFromName(Node.PartiesFromNameRequest.newBuilder()
                .setName(it)
                .setExactMatch(false)
                .build()).partiesList.single()
        }

        val response = genesisService.createGenesis(Genesis.CreateGenesisRequest.newBuilder()
                .addAllParticipants(participants)
                .build())

        val baseId = response.baseId

        File("../", baseHashFilePath).writeText(baseId.txhash.bytes.toByteArray().toHex())
    }

    private fun createHost(endpoint: String) {
        val channel = connectGrpc(endpoint)
        val hostService = HostServiceGrpc.newBlockingStub(channel)
        hostService.createHost(Empty.getDefaultInstance())
    }

    private fun createBank(endpoint: String) {
        val channel = connectGrpc(endpoint)
        val bankService = BankServiceGrpc.newBlockingStub(channel)
        bankService.createBank(Empty.getDefaultInstance())
    }

    private fun allocateFund(endpoint: String, partyName: String) {
        val channel = connectGrpc(endpoint)
        val bankService = BankServiceGrpc.newBlockingStub(channel)
        bankService.allocateFund(BankProto.AllocateFundRequest.newBuilder()
                .setOwner(partyName)
                .setDenom("USD")
                .setAmount("100")
                .build())
    }

    private const val CLIENT_A = "CLIENT_A"
    private const val CLIENT_B = "CLIENT_B"
    private const val CONNECTION_A = "CONNECTION_A"
    private const val CONNECTION_B = "CONNECTION_B"
    private const val PORT_A = "transfer"
    private const val PORT_B = "transfer"
    private const val CHANNEL_A = "CHANNEL_A"
    private const val CHANNEL_B = "CHANNEL_B"
    private const val CHANNEL_VERSION_A = "CHANNEL_VERSION_A"
    private const val CHANNEL_VERSION_B = "CHANNEL_VERSION_B"

    private fun executeTest(endpointA: String, endpointB: String, partyNameA: String, partyNameB: String) {
        val channelA = connectGrpc(endpointA)
        val channelB = connectGrpc(endpointB)

        val hostServiceA = HostServiceGrpc.newBlockingStub(channelA)
        val clientQueryServiceA = ClientQueryGrpc.newBlockingStub(channelA)
        val clientTxServiceA = ClientMsgGrpc.newBlockingStub(channelA)
        val connectionQueryServiceA = ConnectionQueryGrpc.newBlockingStub(channelA)
        val connectionTxServiceA = ConnectionMsgGrpc.newBlockingStub(channelA)
        val channelQueryServiceA = ChannelQueryGrpc.newBlockingStub(channelA)
        val channelTxServiceA = ChannelMsgGrpc.newBlockingStub(channelA)
        val transferTxServiceA = TransferMsgGrpc.newBlockingStub(channelA)

        val hostServiceB = HostServiceGrpc.newBlockingStub(channelB)
        val clientQueryServiceB = ClientQueryGrpc.newBlockingStub(channelB)
        val clientTxServiceB = ClientMsgGrpc.newBlockingStub(channelB)
        val connectionQueryServiceB = ConnectionQueryGrpc.newBlockingStub(channelB)
        val connectionTxServiceB = ConnectionMsgGrpc.newBlockingStub(channelB)
        val channelQueryServiceB = ChannelQueryGrpc.newBlockingStub(channelB)
        val channelTxServiceB = ChannelMsgGrpc.newBlockingStub(channelB)
        val transferTxServiceB = TransferMsgGrpc.newBlockingStub(channelB)

        val hostA = hostServiceA.queryHost(Empty.getDefaultInstance()).into()
        val consensusStateA = hostA.getConsensusState(hostA.getCurrentHeight())

        val hostB = hostServiceB.queryHost(Empty.getDefaultInstance()).into()
        val consensusStateB = hostB.getConsensusState(hostB.getCurrentHeight())

        // createClient @ A
        clientTxServiceA.createClient(MsgCreateClient.newBuilder()
                .setClientId(CLIENT_A)
                .setClientState(Any.pack(Corda.ClientState.newBuilder().setId(CLIENT_A).build(), ""))
                .setConsensusState(consensusStateB.consensusState)
                .build())
        // createClient @ B
        clientTxServiceB.createClient(MsgCreateClient.newBuilder()
                .setClientId(CLIENT_B)
                .setClientState(Any.pack(Corda.ClientState.newBuilder().setId(CLIENT_B).build(), ""))
                .setConsensusState(consensusStateA.consensusState)
                .build())

        // connOpenInit @ A
        val versionA = hostA.getCompatibleVersions().single()
        connectionTxServiceA.connectionOpenInit(MsgConnectionOpenInit.newBuilder().apply {
            clientId = CLIENT_A
            connectionId = CONNECTION_A
            counterpartyBuilder.clientId = CLIENT_B
            counterpartyBuilder.connectionId = ""
            counterpartyBuilder.prefix = hostB.getCommitmentPrefix()
            version = versionA
        }.build())

        // connOpenTry @ B
        val connInit = connectionQueryServiceA.connection(QueryConnectionRequest.newBuilder()
                .setConnectionId(CONNECTION_A)
                .build())
        val clientInit = clientQueryServiceA.clientState(QueryClientStateRequest.newBuilder()
                .setClientId(CLIENT_A)
                .build())
        val consensusInit = clientQueryServiceA.consensusState(QueryConsensusStateRequest.newBuilder()
                .setClientId(CLIENT_A)
                .setLatestHeight(true)
                .build())
        assert(connInit.proofHeight == clientInit.proofHeight)
        assert(connInit.proofHeight == consensusInit.proofHeight)
        connectionTxServiceB.connectionOpenTry(MsgConnectionOpenTry.newBuilder().apply {
            clientId = CLIENT_B
            desiredConnectionId = CONNECTION_B
            counterpartyChosenConnectionId = ""
            clientState = clientInit.clientState
            counterpartyBuilder.clientId = CLIENT_A
            counterpartyBuilder.connectionId = CONNECTION_A
            counterpartyBuilder.prefix = hostA.getCommitmentPrefix()
            addAllCounterpartyVersions(hostA.getCompatibleVersions())
            proofHeight = connInit.proofHeight
            proofInit = connInit.proof
            proofClient = clientInit.proof
            proofConsensus = consensusInit.proof
            consensusHeight = hostB.getCurrentHeight()
        }.build())

        // connOpenAck @ A
        val connTry = connectionQueryServiceB.connection(QueryConnectionRequest.newBuilder()
                .setConnectionId(CONNECTION_B)
                .build())
        val clientTry = clientQueryServiceB.clientState(QueryClientStateRequest.newBuilder()
                .setClientId(CLIENT_B)
                .build())
        val consensusTry = clientQueryServiceB.consensusState(QueryConsensusStateRequest.newBuilder()
                .setClientId(CLIENT_B)
                .setLatestHeight(true)
                .build())
        assert(connTry.proofHeight == clientTry.proofHeight)
        assert(connTry.proofHeight == consensusTry.proofHeight)
        connectionTxServiceA.connectionOpenAck(MsgConnectionOpenAck.newBuilder().apply {
            connectionId = CONNECTION_A
            counterpartyConnectionId = CONNECTION_B
            version = versionA
            clientState = clientTry.clientState
            proofHeight = connTry.proofHeight
            proofTry = connTry.proof
            proofClient = clientTry.proof
            proofConsensus = consensusTry.proof
            consensusHeight = hostA.getCurrentHeight()
        }.build())

        // connOpenConfirm @ B
        val connAck = connectionQueryServiceA.connection(QueryConnectionRequest.newBuilder()
                .setConnectionId(CONNECTION_A)
                .build())
        connectionTxServiceB.connectionOpenConfirm(MsgConnectionOpenConfirm.newBuilder().apply{
            connectionId = CONNECTION_B
            proofAck = connAck.proof
            proofHeight = connAck.proofHeight
        }.build())

        // chanOpenInit @ A
        channelTxServiceA.channelOpenInit(MsgChannelOpenInit.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            channelBuilder.ordering = Order.ORDER_ORDERED
            channelBuilder.counterpartyBuilder.portId = PORT_B
            channelBuilder.counterpartyBuilder.channelId = ""
            channelBuilder.addAllConnectionHops(listOf(CONNECTION_A))
            channelBuilder.version = CHANNEL_VERSION_A
        }.build())

        // chanOpenTry @ B
        val chanInit = channelQueryServiceA.channel(QueryChannelRequest.newBuilder()
                .setPortId(PORT_A)
                .setChannelId(CHANNEL_A)
                .build())
        channelTxServiceB.channelOpenTry(MsgChannelOpenTry.newBuilder().apply{
            portId = PORT_B
            desiredChannelId = CHANNEL_B
            counterpartyChosenChannelId = ""
            channelBuilder.ordering = Order.ORDER_ORDERED
            channelBuilder.counterpartyBuilder.portId = PORT_A
            channelBuilder.counterpartyBuilder.channelId = CHANNEL_A
            channelBuilder.addAllConnectionHops(listOf(CONNECTION_B))
            channelBuilder.version = CHANNEL_VERSION_B
            counterpartyVersion = CHANNEL_VERSION_A
            proofInit = chanInit.proof
            proofHeight = chanInit.proofHeight
        }.build())

        // chanOpenAck @ A
        val chanTry = channelQueryServiceB.channel(QueryChannelRequest.newBuilder()
                .setPortId(PORT_B)
                .setChannelId(CHANNEL_B)
                .build())
        channelTxServiceA.channelOpenAck(MsgChannelOpenAck.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            counterpartyChannelId = CHANNEL_B
            counterpartyVersion = CHANNEL_VERSION_B
            proofTry = chanTry.proof
            proofHeight = chanTry.proofHeight
        }.build())

        // chanOpenConfirm @ B
        val chanAck = channelQueryServiceA.channel(QueryChannelRequest.newBuilder()
                .setPortId(PORT_A)
                .setChannelId(CHANNEL_A)
                .build())
        channelTxServiceB.channelOpenConfirm(MsgChannelOpenConfirm.newBuilder().apply{
            portId = PORT_B
            channelId = CHANNEL_B
            proofAck = chanAck.proof
            proofHeight = chanAck.proofHeight
        }.build())

        val pageReq = Pagination.PageRequest.newBuilder().apply{
            key = ByteString.copyFrom("", Charsets.US_ASCII)
            offset = 0
            limit = 1000
            countTotal = true
        }.build()

        // transfer $10, $20 and $30 @ A
        listOf(10, 20, 30).forEach{
            val amount = it.toString()
            transferTxServiceA.transfer(Tx.MsgTransfer.newBuilder().apply {
                sourcePort = PORT_A
                sourceChannel = CHANNEL_A
                tokenBuilder.denom = "USD"
                tokenBuilder.amount = amount
                sender = partyNameA
                receiver = partyNameB
                timeoutHeight = Height.getDefaultInstance()
                timeoutTimestamp = 0
            }.build())
        }

        // check packet commitments
        channelQueryServiceA.packetCommitments(QueryPacketCommitmentsRequest.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            pagination = pageReq
        }.build()).let { res ->
            assert(res.pagination.total == 3L)
            assert(res.commitmentsCount == 3)
            res.commitmentsList.forEach{ packetState ->
                assert(packetState.portId == PORT_A)
                assert(packetState.channelId == CHANNEL_A)
                val commitment = channelQueryServiceA.packetCommitment(QueryPacketCommitmentRequest.newBuilder().apply {
                    portId = packetState.portId
                    channelId = packetState.channelId
                    sequence = packetState.sequence
                }.build())
                assert(packetState.data == commitment.commitment)
            }
        }

        // check unreceived packets (before recv)
        channelQueryServiceB.unreceivedPackets(QueryUnreceivedPacketsRequest.newBuilder().apply{
            portId = PORT_B
            channelId = CHANNEL_B
            addAllPacketCommitmentSequences(listOf(1, 2, 3))
        }.build()).let {
            assert(it.sequencesCount == 3)
            assert(it.sequencesList.containsAll(listOf(1L, 2L, 3L)))
        }

        // recv $10, $20 and $30 @ B
        repeat(3) {
            val seq = (it + 1).toLong()
            val packetCommitment = channelQueryServiceA.packetCommitment(QueryPacketCommitmentRequest.newBuilder().apply {
                portId = PORT_A
                channelId = CHANNEL_A
                sequence = seq
            }.build())
            channelTxServiceB.recvPacket(MsgRecvPacket.newBuilder().apply {
                packet = Packet.parseFrom(packetCommitment.commitment)
                proof = packetCommitment.proof
                proofHeight = packetCommitment.proofHeight
            }.build())
        }

        // check unreceived packets (after recv)
        channelQueryServiceB.unreceivedPackets(QueryUnreceivedPacketsRequest.newBuilder().apply{
            portId = PORT_B
            channelId = CHANNEL_B
            addAllPacketCommitmentSequences(listOf(1, 2, 3))
        }.build()).let {
            assert(it.sequencesCount == 0)
        }

        // check unreceived acknowledgements (before recv)
        channelQueryServiceA.unreceivedAcks(QueryUnreceivedAcksRequest.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            addAllPacketAckSequences(listOf(1, 2, 3))
        }.build()).let {
            assert(it.sequencesCount == 3)
            assert(it.sequencesList.containsAll(listOf(1L, 2L, 3L)))
        }

        // recv acks for $10, $20 and $30 @ A
        repeat(3) {
            val seq = (it + 1).toLong()
            val packetCommitment = channelQueryServiceA.packetCommitment(QueryPacketCommitmentRequest.newBuilder().apply {
                portId = PORT_A
                channelId = CHANNEL_A
                sequence = seq
            }.build())
            val packetAcknowledgement = channelQueryServiceB.packetAcknowledgement(QueryPacketAcknowledgementRequest.newBuilder().apply {
                portId = PORT_B
                channelId = CHANNEL_B
                sequence = seq
            }.build())
            channelTxServiceA.acknowledgement(MsgAcknowledgement.newBuilder().apply {
                packet = Packet.parseFrom(packetCommitment.commitment)
                acknowledgement = packetAcknowledgement.acknowledgement
                proof = packetAcknowledgement.proof
                proofHeight = packetAcknowledgement.proofHeight
            }.build())
        }

        // check unreceived acknowledgements (after recv)
        channelQueryServiceA.unreceivedAcks(QueryUnreceivedAcksRequest.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            addAllPacketAckSequences(listOf(1, 2, 3))
        }.build()).let {
            assert(it.sequencesCount == 0)
        }

        // check acks
        channelQueryServiceB.packetAcknowledgements(QueryPacketAcknowledgementsRequest.newBuilder().apply{
            portId = PORT_B
            channelId = CHANNEL_B
            pagination = pageReq
        }.build()).let { res ->
            assert(res.pagination.total == 3L)
            assert(res.acknowledgementsCount == 3)
            res.acknowledgementsList.forEach{ packetState ->
                assert(packetState.portId == PORT_B)
                assert(packetState.channelId == CHANNEL_B)
                val ack = channelQueryServiceB.packetAcknowledgement(QueryPacketAcknowledgementRequest.newBuilder().apply {
                    portId = packetState.portId
                    channelId = packetState.channelId
                    sequence = packetState.sequence
                }.build())
                assert(packetState.data == ack.acknowledgement)
            }
        }

        listOf(1, 58, 1).forEachIndexed { i, amount ->
            val seq = (i + 1).toLong()
            val amount = amount.toString()

            // transfer back @ B
            transferTxServiceB.transfer(Tx.MsgTransfer.newBuilder().apply {
                sourcePort = PORT_B
                sourceChannel = CHANNEL_B
                tokenBuilder.denom = Denom("USD")
                        .addPrefix(Identifier(PORT_B), Identifier(CHANNEL_B))
                        .ibcDenom
                        .denom
                tokenBuilder.amount = amount
                sender = partyNameB
                receiver = partyNameA
                timeoutHeight = Height.getDefaultInstance()
                timeoutTimestamp = 0
            }.build())

            // recv  A
            val packetCommitment = channelQueryServiceB.packetCommitment(QueryPacketCommitmentRequest.newBuilder().apply {
                portId = PORT_B
                channelId = CHANNEL_B
                sequence = seq
            }.build())
            channelTxServiceA.recvPacket(MsgRecvPacket.newBuilder().apply {
                packet = Packet.parseFrom(packetCommitment.commitment)
                proof = packetCommitment.proof
                proofHeight = packetCommitment.proofHeight
            }.build())

            // recv ack @ B
            val packetAcknowledgement = channelQueryServiceA.packetAcknowledgement(QueryPacketAcknowledgementRequest.newBuilder().apply {
                portId = PORT_A
                channelId = CHANNEL_A
                sequence = seq
            }.build())
            channelTxServiceB.acknowledgement(MsgAcknowledgement.newBuilder().apply {
                packet = Packet.parseFrom(packetCommitment.commitment)
                acknowledgement = packetAcknowledgement.acknowledgement
                proof = packetAcknowledgement.proof
                proofHeight = packetAcknowledgement.proofHeight
            }.build())
        }
    }
}