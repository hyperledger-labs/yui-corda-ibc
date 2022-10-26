package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import cosmos.base.query.v1beta1.Pagination
import ibc.core.client.v1.Client.Height
import ibc.core.channel.v1.ChannelOuterClass.Order
import ibc.core.channel.v1.ChannelOuterClass.Packet
import ibc.lightclients.corda.v1.*
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import jp.datachain.corda.ibc.clients.corda.toSignedTransaction
import jp.datachain.corda.ibc.conversion.pack
import jp.datachain.corda.ibc.conversion.toCorda
import jp.datachain.corda.ibc.conversion.toProto
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics23.CommitmentProof
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
            "executeTest" -> executeTest(args[1], args[2], args[3], args[4], args[5])
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
            nodeService.partyFromName(Node.PartyFromNameRequest.newBuilder()
                .setName(it)
                .setExactMatch(false)
                .build()).party
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
        val request = HostProto.CreateHostRequest.getDefaultInstance()
        val response = hostService.createHost(request)
        val stx = CommitmentProof(response.proof).toSignedTransaction()
        stx.verifyRequiredSignatures()
    }

    private fun createBank(endpoint: String) {
        val channel = connectGrpc(endpoint)
        val bankService = BankServiceGrpc.newBlockingStub(channel)
        val request = BankProto.CreateBankRequest.getDefaultInstance()
        val response = bankService.createBank(request)
        val stx = CommitmentProof(response.proof).toSignedTransaction()
        stx.verifyRequiredSignatures()
    }

    private fun allocateFund(endpoint: String, partyName: String) {
        val channel = connectGrpc(endpoint)
        val bankService = BankServiceGrpc.newBlockingStub(channel)
        val request = BankProto.AllocateFundRequest.newBuilder()
                .setOwner(partyName)
                .setDenom("USD")
                .setAmount("100")
                .build()
        val response = bankService.allocateFund(request)
        val stx = CommitmentProof(response.proof).toSignedTransaction()
        stx.verifyRequiredSignatures()
    }

    private const val CLIENT_A = "corda-ibc-0"
    private const val CLIENT_B = "corda-ibc-0"
    private const val CONNECTION_A = "connection-0"
    private const val CONNECTION_B = "connection-0"
    private const val PORT_A = "transfer"
    private const val PORT_B = "transfer"
    private const val CHANNEL_A = "channel-0"
    private const val CHANNEL_B = "channel-0"
    private const val CHANNEL_VERSION_A = "CHANNEL_VERSION_A"
    private const val CHANNEL_VERSION_B = "CHANNEL_VERSION_B"

    private fun executeTest(endpointA: String, endpointB: String, endpointBankA: String, partyNameA: String, partyNameB: String) {
        val channelA = connectGrpc(endpointA)
        val channelB = connectGrpc(endpointB)
        val channelBankA = connectGrpc(endpointBankA)

        val hostServiceA = HostServiceGrpc.newBlockingStub(channelA)
        val cashBankServiceA = CashBankServiceGrpc.newBlockingStub(channelA)
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

        val channelQueryServiceBankA = ChannelQueryGrpc.newBlockingStub(channelBankA)
        val channelTxServiceBankA = ChannelMsgGrpc.newBlockingStub(channelBankA)

        val hostA = hostServiceA.queryHost(HostProto.QueryHostRequest.getDefaultInstance()).host.toCorda()
        val consensusStateA = hostA.getConsensusState(hostA.getCurrentHeight())

        val hostB = hostServiceB.queryHost(HostProto.QueryHostRequest.getDefaultInstance()).host.toCorda()
        val consensusStateB = hostB.getConsensusState(hostB.getCurrentHeight())

        // createClient @ A
        clientTxServiceA.createClient(TxClient.CreateClientRequest.newBuilder().apply {
            requestBuilder.clientState = Corda.ClientState.newBuilder()
                    .setBaseId(hostB.baseId.toProto())
                    .setNotaryKey(hostB.notary.owningKey.toProto())
                    .build()
                    .pack()
            requestBuilder.consensusState = consensusStateB.anyConsensusState
        }.build())
        // createClient @ B
        clientTxServiceB.createClient(TxClient.CreateClientRequest.newBuilder().apply {
            requestBuilder.clientState = Corda.ClientState.newBuilder()
                    .setBaseId(hostA.baseId.toProto())
                    .setNotaryKey(hostA.notary.owningKey.toProto())
                    .build()
                    .pack()
            requestBuilder.consensusState = consensusStateA.anyConsensusState
        }.build())

        // connOpenInit @ A
        val versionA = hostA.getCompatibleVersions().single()
        connectionTxServiceA.connectionOpenInit(TxConnection.ConnectionOpenInitRequest.newBuilder().apply {
            requestBuilder.clientId = CLIENT_A
            requestBuilder.counterpartyBuilder.clientId = CLIENT_B
            requestBuilder.counterpartyBuilder.connectionId = ""
            requestBuilder.counterpartyBuilder.prefix = hostB.getCommitmentPrefix()
            requestBuilder.version = versionA
            requestBuilder.delayPeriod = 0
        }.build())

        // connOpenTry @ B
        val connInit = connectionQueryServiceA.connection(QueryConnection.QueryConnectionRequest.newBuilder().apply {
            requestBuilder.connectionId = CONNECTION_A
        }.build()).response
        val clientInit = clientQueryServiceA.clientState(QueryClient.QueryClientStateRequest.newBuilder().apply {
            requestBuilder.clientId = CLIENT_A
        }.build()).response
        val consensusInit = clientQueryServiceA.consensusState(QueryClient.QueryConsensusStateRequest.newBuilder().apply {
            requestBuilder.clientId = CLIENT_A
            requestBuilder.latestHeight = true
        }.build()).response
        require(connInit.proofHeight == clientInit.proofHeight)
        require(connInit.proofHeight == consensusInit.proofHeight)
        connectionTxServiceB.connectionOpenTry(TxConnection.ConnectionOpenTryRequest.newBuilder().apply {
            requestBuilder.clientId = CLIENT_B
            requestBuilder.clientState = clientInit.clientState
            requestBuilder.counterpartyBuilder.clientId = CLIENT_A
            requestBuilder.counterpartyBuilder.connectionId = CONNECTION_A
            requestBuilder.counterpartyBuilder.prefix = hostA.getCommitmentPrefix()
            requestBuilder.delayPeriod = 0
            requestBuilder.addAllCounterpartyVersions(hostA.getCompatibleVersions())
            requestBuilder.proofHeight = connInit.proofHeight
            requestBuilder.proofInit = connInit.proof
            requestBuilder.proofClient = clientInit.proof
            requestBuilder.proofConsensus = consensusInit.proof
            requestBuilder.consensusHeight = hostB.getCurrentHeight()
        }.build())

        // connOpenAck @ A
        val connTry = connectionQueryServiceB.connection(QueryConnection.QueryConnectionRequest.newBuilder().apply {
            requestBuilder.connectionId = CONNECTION_B
        }.build()).response
        val clientTry = clientQueryServiceB.clientState(QueryClient.QueryClientStateRequest.newBuilder().apply {
            requestBuilder.clientId = CLIENT_B
        }.build()).response
        val consensusTry = clientQueryServiceB.consensusState(QueryClient.QueryConsensusStateRequest.newBuilder().apply {
            requestBuilder.clientId = CLIENT_B
            requestBuilder.latestHeight = true
        }.build()).response
        require(connTry.proofHeight == clientTry.proofHeight)
        require(connTry.proofHeight == consensusTry.proofHeight)
        connectionTxServiceA.connectionOpenAck(TxConnection.ConnectionOpenAckRequest.newBuilder().apply {
            requestBuilder.connectionId = CONNECTION_A
            requestBuilder.counterpartyConnectionId = CONNECTION_B
            requestBuilder.version = versionA
            requestBuilder.clientState = clientTry.clientState
            requestBuilder.proofHeight = connTry.proofHeight
            requestBuilder.proofTry = connTry.proof
            requestBuilder.proofClient = clientTry.proof
            requestBuilder.proofConsensus = consensusTry.proof
            requestBuilder.consensusHeight = hostA.getCurrentHeight()
        }.build())

        // connOpenConfirm @ B
        val connAck = connectionQueryServiceA.connection(QueryConnection.QueryConnectionRequest.newBuilder().apply {
            requestBuilder.connectionId = CONNECTION_A
        }.build()).response
        connectionTxServiceB.connectionOpenConfirm(TxConnection.ConnectionOpenConfirmRequest.newBuilder().apply {
            requestBuilder.connectionId = CONNECTION_B
            requestBuilder.proofAck = connAck.proof
            requestBuilder.proofHeight = connAck.proofHeight
        }.build())

        // chanOpenInit @ A
        channelTxServiceA.channelOpenInit(TxChannel.ChannelOpenInitRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelBuilder.ordering = Order.ORDER_ORDERED
            requestBuilder.channelBuilder.counterpartyBuilder.portId = PORT_B
            requestBuilder.channelBuilder.counterpartyBuilder.channelId = ""
            requestBuilder.channelBuilder.addAllConnectionHops(listOf(CONNECTION_A))
            requestBuilder.channelBuilder.version = CHANNEL_VERSION_A
        }.build())

        // chanOpenTry @ B
        val chanInit = channelQueryServiceA.channel(QueryChannel.QueryChannelRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelId = CHANNEL_A
        }.build()).response
        channelTxServiceB.channelOpenTry(TxChannel.ChannelOpenTryRequest.newBuilder().apply {
            requestBuilder.portId = PORT_B
            requestBuilder.channelBuilder.ordering = Order.ORDER_ORDERED
            requestBuilder.channelBuilder.counterpartyBuilder.portId = PORT_A
            requestBuilder.channelBuilder.counterpartyBuilder.channelId = CHANNEL_A
            requestBuilder.channelBuilder.addAllConnectionHops(listOf(CONNECTION_B))
            requestBuilder.channelBuilder.version = CHANNEL_VERSION_B
            requestBuilder.counterpartyVersion = CHANNEL_VERSION_A
            requestBuilder.proofInit = chanInit.proof
            requestBuilder.proofHeight = chanInit.proofHeight
        }.build())

        // chanOpenAck @ A
        val chanTry = channelQueryServiceB.channel(QueryChannel.QueryChannelRequest.newBuilder().apply {
            requestBuilder.portId = PORT_B
            requestBuilder.channelId = CHANNEL_B
        }.build()).response
        channelTxServiceA.channelOpenAck(TxChannel.ChannelOpenAckRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelId = CHANNEL_A
            requestBuilder.counterpartyChannelId = CHANNEL_B
            requestBuilder.counterpartyVersion = CHANNEL_VERSION_B
            requestBuilder.proofTry = chanTry.proof
            requestBuilder.proofHeight = chanTry.proofHeight
        }.build())

        // chanOpenConfirm @ B
        val chanAck = channelQueryServiceA.channel(QueryChannel.QueryChannelRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelId = CHANNEL_A
        }.build()).response
        channelTxServiceB.channelOpenConfirm(TxChannel.ChannelOpenConfirmRequest.newBuilder().apply {
            requestBuilder.portId = PORT_B
            requestBuilder.channelId = CHANNEL_B
            requestBuilder.proofAck = chanAck.proof
            requestBuilder.proofHeight = chanAck.proofHeight
        }.build())

        val cashBankKey = cashBankServiceA.queryCashBank(CashBankProto.QueryCashBankRequest.getDefaultInstance()).cashBank.owner.owningKey.toCorda().encoded.toHex()
        val baseDenom = "USD$cashBankKey"

        val pageReq = Pagination.PageRequest.newBuilder().apply{
            key = ByteString.copyFrom("", Charsets.US_ASCII)
            offset = 0
            limit = 1000
            countTotal = true
        }.build()

        // transfer $10, $20 and $30 @ A
        listOf(10, 20, 30).forEach{
            val amount = it.toString()
            transferTxServiceA.transfer(TxTransfer.TransferRequest.newBuilder().apply {
                requestBuilder.sourcePort = PORT_A
                requestBuilder.sourceChannel = CHANNEL_A
                requestBuilder.tokenBuilder.denom = baseDenom
                requestBuilder.tokenBuilder.amount = amount
                requestBuilder.sender = partyNameA
                requestBuilder.receiver = partyNameB
                requestBuilder.timeoutHeight = Height.getDefaultInstance()
                requestBuilder.timeoutTimestamp = 0
            }.build())
        }

        // check packet commitments
        channelQueryServiceA.packetCommitments(QueryChannel.QueryPacketCommitmentsRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelId = CHANNEL_A
            requestBuilder.pagination = pageReq
        }.build()).response.let { res ->
            require(res.pagination.total == 3L)
            require(res.commitmentsCount == 3)
            res.commitmentsList.forEach{ packetState ->
                require(packetState.portId == PORT_A)
                require(packetState.channelId == CHANNEL_A)
                val packetCommitment = channelQueryServiceA.packetCommitment(QueryChannel.QueryPacketCommitmentRequest.newBuilder().apply {
                    requestBuilder.portId = packetState.portId
                    requestBuilder.channelId = packetState.channelId
                    requestBuilder.sequence = packetState.sequence
                }.build()).response
                require(packetState.data == packetCommitment.commitment)
            }
        }

        // check unreceived packets (before recv)
        channelQueryServiceB.unreceivedPackets(QueryChannel.QueryUnreceivedPacketsRequest.newBuilder().apply {
            requestBuilder.portId = PORT_B
            requestBuilder.channelId = CHANNEL_B
            requestBuilder.addAllPacketCommitmentSequences(listOf(1, 2, 3))
        }.build()).response.let {
            require(it.sequencesCount == 3)
            require(it.sequencesList.containsAll(listOf(1L, 2L, 3L)))
        }

        // recv $10, $20 and $30 @ B
        repeat(3) {
            val seq = (it + 1).toLong()
            val packetCommitment = channelQueryServiceA.packetCommitment(QueryChannel.QueryPacketCommitmentRequest.newBuilder().apply {
                requestBuilder.portId = PORT_A
                requestBuilder.channelId = CHANNEL_A
                requestBuilder.sequence = seq
            }.build()).response
            channelTxServiceB.recvPacket(TxChannel.RecvPacketRequest.newBuilder().apply {
                requestBuilder.packet = Packet.parseFrom(packetCommitment.commitment)
                requestBuilder.proofCommitment = packetCommitment.proof
                requestBuilder.proofHeight = packetCommitment.proofHeight
            }.build())
        }

        // check unreceived packets (after recv)
        channelQueryServiceB.unreceivedPackets(QueryChannel.QueryUnreceivedPacketsRequest.newBuilder().apply {
            requestBuilder.portId = PORT_B
            requestBuilder.channelId = CHANNEL_B
            requestBuilder.addAllPacketCommitmentSequences(listOf(1, 2, 3))
        }.build()).response.let {
            require(it.sequencesCount == 0)
        }

        // check unreceived acknowledgements (before recv)
        channelQueryServiceA.unreceivedAcks(QueryChannel.QueryUnreceivedAcksRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelId = CHANNEL_A
            requestBuilder.addAllPacketAckSequences(listOf(1, 2, 3))
        }.build()).response.let {
            require(it.sequencesCount == 3)
            require(it.sequencesList.containsAll(listOf(1L, 2L, 3L)))
        }

        // recv acks for $10, $20 and $30 @ A
        repeat(3) {
            val seq = (it + 1).toLong()
            val packetCommitment = channelQueryServiceA.packetCommitment(QueryChannel.QueryPacketCommitmentRequest.newBuilder().apply {
                requestBuilder.portId = PORT_A
                requestBuilder.channelId = CHANNEL_A
                requestBuilder.sequence = seq
            }.build()).response
            val packetAcknowledgement = channelQueryServiceB.packetAcknowledgement(QueryChannel.QueryPacketAcknowledgementRequest.newBuilder().apply {
                requestBuilder.portId = PORT_B
                requestBuilder.channelId = CHANNEL_B
                requestBuilder.sequence = seq
            }.build()).response
            channelTxServiceA.acknowledgement(TxChannel.AcknowledgementRequest.newBuilder().apply {
                requestBuilder.packet = Packet.parseFrom(packetCommitment.commitment)
                requestBuilder.acknowledgement = packetAcknowledgement.acknowledgement
                requestBuilder.proofAcked = packetAcknowledgement.proof
                requestBuilder.proofHeight = packetAcknowledgement.proofHeight
            }.build())
        }

        // check unreceived acknowledgements (after recv)
        channelQueryServiceA.unreceivedAcks(QueryChannel.QueryUnreceivedAcksRequest.newBuilder().apply {
            requestBuilder.portId = PORT_A
            requestBuilder.channelId = CHANNEL_A
            requestBuilder.addAllPacketAckSequences(listOf(1, 2, 3))
        }.build()).response.let {
            require(it.sequencesCount == 0)
        }

        // check acks
        channelQueryServiceB.packetAcknowledgements(QueryChannel.QueryPacketAcknowledgementsRequest.newBuilder().apply {
            requestBuilder.portId = PORT_B
            requestBuilder.channelId = CHANNEL_B
            requestBuilder.pagination = pageReq
        }.build()).response.let { res ->
            require(res.pagination.total == 3L)
            require(res.acknowledgementsCount == 3)
            res.acknowledgementsList.forEach{ packetState ->
                require(packetState.portId == PORT_B)
                require(packetState.channelId == CHANNEL_B)
                val ack = channelQueryServiceB.packetAcknowledgement(QueryChannel.QueryPacketAcknowledgementRequest.newBuilder().apply {
                    requestBuilder.portId = packetState.portId
                    requestBuilder.channelId = packetState.channelId
                    requestBuilder.sequence = packetState.sequence
                }.build()).response
                require(packetState.data == ack.acknowledgement)
            }
        }

        listOf(1, 58, 1).forEachIndexed { i, quantity ->
            val seq = (i + 1).toLong()
            val amount = quantity.toString()

            // transfer back @ B
            transferTxServiceB.transfer(TxTransfer.TransferRequest.newBuilder().apply {
                requestBuilder.sourcePort = PORT_B
                requestBuilder.sourceChannel = CHANNEL_B
                requestBuilder.tokenBuilder.denom = Denom.fromString(baseDenom)
                        .addPath(Identifier(PORT_B), Identifier(CHANNEL_B))
                        .toIbcDenom()
                requestBuilder.tokenBuilder.amount = amount
                requestBuilder.sender = partyNameB
                requestBuilder.receiver = partyNameA
                requestBuilder.timeoutHeight = Height.getDefaultInstance()
                requestBuilder.timeoutTimestamp = 0
            }.build())

            // recv  A
            val packetCommitment = channelQueryServiceB.packetCommitment(QueryChannel.QueryPacketCommitmentRequest.newBuilder().apply {
                requestBuilder.portId = PORT_B
                requestBuilder.channelId = CHANNEL_B
                requestBuilder.sequence = seq
            }.build()).response
            channelTxServiceBankA.recvPacket(TxChannel.RecvPacketRequest.newBuilder().apply {
                requestBuilder.packet = Packet.parseFrom(packetCommitment.commitment)
                requestBuilder.proofCommitment = packetCommitment.proof
                requestBuilder.proofHeight = packetCommitment.proofHeight
            }.build())

            // recv ack @ B
            val packetAcknowledgement = channelQueryServiceBankA.packetAcknowledgement(QueryChannel.QueryPacketAcknowledgementRequest.newBuilder().apply {
                requestBuilder.portId = PORT_A
                requestBuilder.channelId = CHANNEL_A
                requestBuilder.sequence = seq
            }.build()).response
            channelTxServiceB.acknowledgement(TxChannel.AcknowledgementRequest.newBuilder().apply {
                requestBuilder.packet = Packet.parseFrom(packetCommitment.commitment)
                requestBuilder.acknowledgement = packetAcknowledgement.acknowledgement
                requestBuilder.proofAcked = packetAcknowledgement.proof
                requestBuilder.proofHeight = packetAcknowledgement.proofHeight
            }.build())
        }
    }
}
