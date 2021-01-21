package jp.datachain.corda.ibc.grpc_adapter

import com.google.protobuf.Any
import ibc.applications.transfer.v1.Tx
import ibc.core.client.v1.Client.*
import ibc.core.client.v1.QueryOuterClass.*
import ibc.core.connection.v1.Tx.*
import ibc.core.connection.v1.QueryOuterClass.*
import ibc.core.channel.v1.Tx.*
import ibc.core.channel.v1.QueryOuterClass.*
import ibc.core.channel.v1.ChannelOuterClass.*
import ibc.core.client.v1.MsgGrpc as ClientMsgGrpc
import ibc.core.client.v1.QueryGrpc as ClientQueryGrpc
import ibc.core.connection.v1.MsgGrpc as ConnectionMsgGrpc
import ibc.core.connection.v1.QueryGrpc as ConnectionQueryGrpc
import ibc.core.channel.v1.MsgGrpc as ChannelMsgGrpc
import ibc.core.channel.v1.QueryGrpc as ChannelQueryGrpc
import ibc.applications.transfer.v1.MsgGrpc as TransferMsgGrpc
import io.grpc.ManagedChannelBuilder
import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.grpc.*
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.utilities.NetworkHostAndPort
import io.grpc.StatusRuntimeException
import net.corda.core.crypto.SecureHash
import net.corda.core.utilities.toHex
import java.io.File

object Client {
    @JvmStatic
    fun main(args: Array<String>) {
        warmUpSerialization()
        when (args[0]) {
            "shutdown" -> shutdown(args[1])
            "createGenesis" -> createGenesis(args[1], args[2], args[3])
            "createHost" -> createHost(args[1], args[2])
            "allocateFund" -> allocateFund(args[1], args[2], args[3])
            "executeTest" -> executeTest(args[1], args[2], args[3], args[4])
        }
    }

    private fun warmUpSerialization() {
        // port 0 is dummy. This instance of CordaRPCClient is never used.
        // Before using Corda's (de)serialization mechanism, one of four
        // pre-defined serialization environments must be initialized.
        // In the initializer block (init { ... }) of CordaRPCClient,
        // the "node" serialization environment is initialized.
        // Ref: https://github.com/corda/corda/blob/release/os/4.3/client/rpc/src/main/kotlin/net/corda/client/rpc/CordaRPCClient.kt#L435
        CordaRPCClient(NetworkHostAndPort("localhost", 0))
    }

    private fun connectGrpc(endpoint: String) = ManagedChannelBuilder.forTarget(endpoint)
            .usePlaintext()
            .build()

    private fun shutdown(endpoint: String) {
        val channel = connectGrpc(endpoint)
        val adminService = AdminServiceGrpc.newBlockingStub(channel)

        try {
            adminService.shutdown(Void.getDefaultInstance())
        } catch (e: StatusRuntimeException) {
            println(e)
        }
    }

    private fun createGenesis(endpoint: String, partyName: String, baseHashFilePath: String) {
        val channel = connectGrpc(endpoint)
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val partyMap = listOf(partyName, "Notary").map {
            it to
                    nodeService.partiesFromName(Operation.PartiesFromNameRequest.newBuilder()
                            .setName(it)
                            .setExactMatch(false)
                            .build()).partiesList.single()
        }.toMap()

        val stxGenesis = ibcService.createGenesis(Operation.CreateGenesisRequest.newBuilder()
                .addAllParticipants(partyMap.values)
                .build()).into()

        val baseId = StateRef(txhash = stxGenesis.id, index = 0)

        File("../", baseHashFilePath).writeText(baseId.txhash.bytes.toHex())
    }

    private fun createHost(endpoint: String, baseHash: String) {
        val channel = connectGrpc(endpoint)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val baseId = StateRef(txhash = SecureHash.parse(baseHash), index = 0)

        ibcService.createHostAndBank(baseId.into())
    }

    private fun allocateFund(endpoint: String, baseHash: String, partyName: String) {
        val channel = connectGrpc(endpoint)
        val nodeService = NodeServiceGrpc.newBlockingStub(channel)
        val ibcService = IbcServiceGrpc.newBlockingStub(channel)

        val baseId = StateRef(txhash = SecureHash.parse(baseHash), index = 0)

        val party = nodeService.partiesFromName(Operation.PartiesFromNameRequest.newBuilder()
                .setName(partyName)
                .setExactMatch(false)
                .build()).partiesList.single()

        ibcService.allocateFund(Operation.AllocateFundRequest.newBuilder()
                .setBaseId(baseId.into())
                .setOwner(party.owningKey)
                .setDenom("USD")
                .setAmount("100")
                .build()).into()

        println(baseId.txhash.bytes.toHex())
    }

    const val CLIENT_A = "CLIENT_A"
    const val CLIENT_B = "CLIENT_B"
    const val CONNECTION_A = "CONNECTION_A"
    const val CONNECTION_B = "CONNECTION_B"
    const val PORT_A = "transfer"
    const val PORT_B = "transfer"
    const val CHANNEL_A = "CHANNEL_A"
    const val CHANNEL_B = "CHANNEL_B"
    const val CHANNEL_VERSION_A = "CHANNEL_VERSION_A"
    const val CHANNEL_VERSION_B = "CHANNEL_VERSION_B"

    private fun executeTest(endpointA: String, endpointB: String, partyNameA: String, partyNameB: String) {
        val channelA = connectGrpc(endpointA)
        val channelB = connectGrpc(endpointB)

        val hostAndBankQueryServiceA = QueryServiceGrpc.newBlockingStub(channelA)
        val clientQueryServiceA = ClientQueryGrpc.newBlockingStub(channelA)
        val clientTxServiceA = ClientMsgGrpc.newBlockingStub(channelA)
        val connectionQueryServiceA = ConnectionQueryGrpc.newBlockingStub(channelA)
        val connectionTxServiceA = ConnectionMsgGrpc.newBlockingStub(channelA)
        val channelQueryServiceA = ChannelQueryGrpc.newBlockingStub(channelA)
        val channelTxServiceA = ChannelMsgGrpc.newBlockingStub(channelA)
        val transferTxServiceA = TransferMsgGrpc.newBlockingStub(channelA)

        val hostAndBankQueryServiceB = QueryServiceGrpc.newBlockingStub(channelB)
        val clientQueryServiceB = ClientQueryGrpc.newBlockingStub(channelB)
        val clientTxServiceB = ClientMsgGrpc.newBlockingStub(channelB)
        val connectionQueryServiceB = ConnectionQueryGrpc.newBlockingStub(channelB)
        val connectionTxServiceB = ConnectionMsgGrpc.newBlockingStub(channelB)
        val channelQueryServiceB = ChannelQueryGrpc.newBlockingStub(channelB)
        val channelTxServiceB = ChannelMsgGrpc.newBlockingStub(channelB)
        val transferTxServiceB = TransferMsgGrpc.newBlockingStub(channelB)

        val nodeService = NodeServiceGrpc.newBlockingStub(channelA)

        val hostA = hostAndBankQueryServiceA.queryHost(Query.QueryHostRequest.getDefaultInstance()).into()
        val consensusStateA = hostA.getConsensusState(hostA.getCurrentHeight())

        val hostB = hostAndBankQueryServiceB.queryHost(Query.QueryHostRequest.getDefaultInstance()).into()
        val consensusStateB = hostB.getConsensusState(hostB.getCurrentHeight())

        clientTxServiceA.createClient(MsgCreateClient.newBuilder()
                .setClientId(CLIENT_A)
                .setClientState(Any.pack(Corda.ClientState.getDefaultInstance()))
                .setConsensusState(consensusStateB.consensusState)
                .build())
        clientTxServiceB.createClient(MsgCreateClient.newBuilder()
                .setClientId(CLIENT_B)
                .setClientState(Any.pack(Corda.ClientState.getDefaultInstance()))
                .setConsensusState(consensusStateA.consensusState)
                .build())

        // connOpenInit
        val versionA = hostA.getCompatibleVersions().single()
        connectionTxServiceA.connectionOpenInit(MsgConnectionOpenInit.newBuilder().apply {
            clientId = CLIENT_A
            connectionId = CONNECTION_A
            counterpartyBuilder.clientId = CLIENT_B
            counterpartyBuilder.connectionId = ""
            counterpartyBuilder.prefix = hostB.getCommitmentPrefix()
            version = versionA
        }.build())

        // connOpenTry
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

        // connOpenAck
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

        // connOpenConfirm
        val connAck = connectionQueryServiceA.connection(QueryConnectionRequest.newBuilder()
                .setConnectionId(CONNECTION_A)
                .build())
        connectionTxServiceB.connectionOpenConfirm(MsgConnectionOpenConfirm.newBuilder().apply{
            connectionId = CONNECTION_B
            proofAck = connAck.proof
            proofHeight = connAck.proofHeight
        }.build())

        // chanOpenInit
        channelTxServiceA.channelOpenInit(MsgChannelOpenInit.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            channelBuilder.ordering = Order.ORDER_ORDERED
            channelBuilder.counterpartyBuilder.portId = PORT_B
            channelBuilder.counterpartyBuilder.channelId = ""
            channelBuilder.addAllConnectionHops(listOf(CONNECTION_A))
            channelBuilder.version = CHANNEL_VERSION_A
        }.build())

        // chanOpenTry
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

        // chanOpenAck
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

        // chanOpenConfirm
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

        val addrA = nodeService.partiesFromName(Operation.PartiesFromNameRequest.newBuilder()
                .setName(partyNameA)
                .setExactMatch(false)
                .build()).partiesList.single().owningKey.encoded.toByteArray().toHex()
        val addrB = nodeService.partiesFromName(Operation.PartiesFromNameRequest.newBuilder()
                .setName(partyNameB)
                .setExactMatch(false)
                .build()).partiesList.single().owningKey.encoded.toByteArray().toHex()

        // transfer (send) @ A
        transferTxServiceA.transfer(Tx.MsgTransfer.newBuilder().apply{
            sourcePort = PORT_A
            sourceChannel = CHANNEL_A
            tokenBuilder.denom = "USD"
            tokenBuilder.amount = "10"
            sender = addrA
            receiver = addrB
            timeoutHeight = hostB.getCurrentHeight()
            timeoutTimestamp = 0
        }.build())

        // recv @ B
        val packetCommitmentA = channelQueryServiceA.packetCommitment(QueryPacketCommitmentRequest.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            sequence = 1
        }.build())
        channelTxServiceB.recvPacket(MsgRecvPacket.newBuilder().apply{
            packet = Packet.parseFrom(packetCommitmentA.commitment)
            proof = packetCommitmentA.proof
            proofHeight = packetCommitmentA.proofHeight
        }.build())

        // acknowledge @ A
        val packetAcknowledgementB = channelQueryServiceB.packetAcknowledgement(QueryPacketAcknowledgementRequest.newBuilder().apply{
            portId = PORT_B
            channelId = CHANNEL_B
            sequence = 1
        }.build())
        channelTxServiceA.acknowledgement(MsgAcknowledgement.newBuilder().apply{
            packet = Packet.parseFrom(packetCommitmentA.commitment)
            acknowledgement = packetAcknowledgementB.acknowledgement
            proof = packetAcknowledgementB.proof
            proofHeight = packetAcknowledgementB.proofHeight
        }.build())

        // transfer (send) @ B
        transferTxServiceB.transfer(Tx.MsgTransfer.newBuilder().apply{
            sourcePort = PORT_B
            sourceChannel = CHANNEL_B
            tokenBuilder.denom = "$PORT_B/$CHANNEL_B/USD"
            tokenBuilder.amount = "10"
            sender = addrB
            receiver = addrA
            timeoutHeight = hostA.getCurrentHeight()
            timeoutTimestamp = 0
        }.build())

        // recv @ A
        val packetCommitmentB = channelQueryServiceB.packetCommitment(QueryPacketCommitmentRequest.newBuilder().apply{
            portId = PORT_B
            channelId = CHANNEL_B
            sequence = 1
        }.build())
        channelTxServiceA.recvPacket(MsgRecvPacket.newBuilder().apply{
            packet = Packet.parseFrom(packetCommitmentB.commitment)
            proof = packetCommitmentB.proof
            proofHeight = packetCommitmentB.proofHeight
        }.build())

        // acknowledge @ B
        val packetAcknowledgementA = channelQueryServiceA.packetAcknowledgement(QueryPacketAcknowledgementRequest.newBuilder().apply{
            portId = PORT_A
            channelId = CHANNEL_A
            sequence = 1
        }.build())
        channelTxServiceB.acknowledgement(MsgAcknowledgement.newBuilder().apply{
            packet = Packet.parseFrom(packetCommitmentB.commitment)
            acknowledgement = packetAcknowledgementA.acknowledgement
            proof = packetAcknowledgementA.proof
            proofHeight = packetAcknowledgementA.proofHeight
        }.build())
    }
}