package jp.datachain.corda.ibc.flows.test

import com.google.protobuf.ByteString
import ibc.applications.transfer.v1.Tx.*
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.channel.v1.Tx.*
import ibc.core.client.v1.Client.Height
import ibc.core.client.v1.Tx.*
import ibc.core.connection.v1.Tx.*
import ibc.lightclients.corda.v1.Corda
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.conversion.pack
import jp.datachain.corda.ibc.conversion.toProto
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.toJson
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.identity.Party
import net.corda.testing.core.*
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.assertFailsWith

class IbcFlowTests {
    private var maybeNetwork: MockNetwork? = null
    private val network get() = maybeNetwork!!

    private val StartedMockNode.party : Party get() = info.legalIdentities.single()
    private val StartedMockNode.addr : Address get() = Address.fromPublicKey(party.owningKey)

    @Before
    fun setup() {
        val networkParam = MockNetworkParameters(
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("jp.datachain.corda.ibc.contracts"),
                        TestCordapp.findCordapp("jp.datachain.corda.ibc.flows"),
                        TestCordapp.findCordapp("net.corda.finance.contracts"),
                        TestCordapp.findCordapp("net.corda.finance.flows")
                ),
                notarySpecs = listOf(
                        MockNetworkNotarySpec(DUMMY_NOTARY_NAME, validating = true)
                )
        ).let{
            it.withNetworkParameters(it.networkParameters.copy(minimumPlatformVersion = 4))
        }
        maybeNetwork = MockNetwork(networkParam)
    }

    @After
    fun tearDown() {
        network.stopNodes()
        maybeNetwork = null
    }

    @Test
    fun `relayer logic`() {
        val a1 = network.createNode()
        val a2 = network.createNode()
        val b1 = network.createNode()
        val b2 = network.createNode()

        val ibcA = TestCordaIbcClient(network, a1)
        val ibcB = TestCordaIbcClient(network, b1)

        ibcA.createGenesis(listOf(a1.party, a2.party))
        ibcA.createHost()

        ibcB.createGenesis(listOf(b1.party, b2.party))
        ibcB.createHost()

        val expectedClientId = "corda-ibc-0"

        val clientAid = ibcA.createClient(MsgCreateClient.newBuilder().apply{
            clientState = Corda.ClientState.newBuilder().apply{
                baseId = ibcB.host().baseId.toProto()
                notaryKey = ibcB.host().notary.owningKey.toProto()
            }.build().pack()
            consensusState = ibcB.host().getConsensusState(HEIGHT).anyConsensusState
        }.build())
        assert(clientAid.id == expectedClientId)

        val clientBid = ibcB.createClient(MsgCreateClient.newBuilder().apply{
            clientState = Corda.ClientState.newBuilder().apply{
                baseId = ibcA.host().baseId.toProto()
                notaryKey = ibcA.host().notary.owningKey.toProto()
            }.build().pack()
            consensusState = ibcA.host().getConsensusState(HEIGHT).anyConsensusState
        }.build())
        assert(clientAid.id == expectedClientId)

        val connAid = ibcA.connOpenInit(MsgConnectionOpenInit.newBuilder().apply{
            clientId = clientAid.id
            counterpartyBuilder.clientId = clientBid.id
            counterpartyBuilder.prefix = ibcB.host().getCommitmentPrefix()
            version = ibcA.host().getCompatibleVersions().single()
            delayPeriod = 0
        }.build())

        val connBid = ibcB.connOpenTry(MsgConnectionOpenTry.newBuilder().apply{
            clientId = clientBid.id
            previousConnectionId = ""
            clientState = ibcA.client(clientAid).anyClientState
            counterpartyBuilder.clientId = clientAid.id
            counterpartyBuilder.connectionId = connAid.id
            counterpartyBuilder.prefix = ibcA.host().getCommitmentPrefix()
            delayPeriod = 0
            addAllCounterpartyVersions(ibcA.host().getCompatibleVersions())
            proofHeight = ibcA.host().getCurrentHeight()
            proofInit = ibcA.connProof(connAid).toByteString()
            proofClient = ibcA.clientProof(clientAid).toByteString()
            proofConsensus = ibcA.clientProof(clientAid).toByteString()
            consensusHeight = ibcB.host().getCurrentHeight()
        }.build())

        ibcA.connOpenAck(MsgConnectionOpenAck.newBuilder().apply{
            connectionId = connAid.id
            counterpartyConnectionId = connBid.id
            version = ibcB.conn(connBid).end.versionsList.single()
            clientState = ibcB.client(clientBid).anyClientState
            proofHeight = ibcB.host().getCurrentHeight()
            proofTry = ibcB.connProof(connBid).toByteString()
            proofClient = ibcB.clientProof(clientBid).toByteString()
            proofConsensus = ibcB.clientProof(clientBid).toByteString()
            consensusHeight = ibcA.host().getCurrentHeight()
        }.build())

        ibcB.connOpenConfirm(MsgConnectionOpenConfirm.newBuilder().apply{
            connectionId = connBid.id
            proofAck = ibcA.connProof(connAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        val portAid = Identifier("portA")
        val portBid = Identifier("portB")
        val channelVersionA = "CHANNEL_VERSION_A" // arbitrary string is ok
        val channelVersionB = "CHANNEL_VERSION_B" // arbitrary string is ok
        val ordering = ChannelOuterClass.Order.ORDER_ORDERED

        val chanAid = ibcA.chanOpenInit(MsgChannelOpenInit.newBuilder().apply{
            portId = portAid.id
            channelBuilder.state = ChannelOuterClass.State.STATE_UNINITIALIZED_UNSPECIFIED
            channelBuilder.ordering = ordering
            channelBuilder.counterpartyBuilder.portId = portBid.id
            channelBuilder.counterpartyBuilder.channelId = ""
            channelBuilder.addAllConnectionHops(listOf(connAid.id))
            channelBuilder.version = channelVersionA
        }.build())

        val chanBid = ibcB.chanOpenTry(MsgChannelOpenTry.newBuilder().apply{
            portId = portBid.id
            previousChannelId = ""
            channelBuilder.state = ChannelOuterClass.State.STATE_UNINITIALIZED_UNSPECIFIED
            channelBuilder.ordering = ordering
            channelBuilder.counterpartyBuilder.portId = portAid.id
            channelBuilder.counterpartyBuilder.channelId = chanAid.id
            channelBuilder.addAllConnectionHops(listOf(connBid.id))
            channelBuilder.version = channelVersionB
            counterpartyVersion = ibcA.chan(chanAid).end.version
            proofInit = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        ibcA.chanOpenAck(MsgChannelOpenAck.newBuilder().apply{
            portId = portAid.id
            channelId = chanAid.id
            counterpartyChannelId = chanBid.id
            counterpartyVersion = ibcB.chan(chanBid).end.version
            proofTry = ibcB.chanProof(chanBid).toByteString()
            proofHeight = ibcB.host().getCurrentHeight()
        }.build())

        ibcB.chanOpenConfirm(MsgChannelOpenConfirm.newBuilder().apply{
            portId = portBid.id
            channelId = chanBid.id
            proofAck = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        for (sequence in 1L..10) {
            val packet = ChannelOuterClass.Packet.newBuilder()
                    .setSequence(sequence)
                    .setSourcePort(portAid.id)
                    .setSourceChannel(chanAid.id)
                    .setDestinationPort(portBid.id)
                    .setDestinationChannel(chanBid.id)
                    .setData(ByteString.copyFromUtf8("Hello, Bob! (${sequence})"))
                    .setTimeoutHeight(Height.getDefaultInstance())
                    .setTimeoutTimestamp(0)
                    .build()

            ibcA.sendPacket(packet)

            ibcB.recvPacketOrdered(MsgRecvPacket.newBuilder()
                    .setPacket(packet)
                    .setProofCommitment(ibcA.chanProof(chanAid).toByteString())
                    .setProofHeight(ibcA.host().getCurrentHeight())
                    .build())

            val ack = ibcB.chan(chanBid).acknowledgements[sequence]!!.toJson()

            ibcA.acknowledgePacketOrdered(MsgAcknowledgement.newBuilder()
                    .setPacket(packet)
                    .setAcknowledgement(ack)
                    .setProofAcked(ibcB.chanProof(chanBid).toByteString())
                    .setProofHeight(ibcB.host().getCurrentHeight())
                    .build())
        }

        for (sequence in 1L..10) {
            val packet = ChannelOuterClass.Packet.newBuilder()
                    .setSequence(sequence)
                    .setSourcePort(portBid.id)
                    .setSourceChannel(chanBid.id)
                    .setDestinationPort(portAid.id)
                    .setDestinationChannel(chanAid.id)
                    .setData(ByteString.copyFromUtf8("Hello, Alice! (${sequence})"))
                    .setTimeoutHeight(Height.getDefaultInstance())
                    .setTimeoutTimestamp(0)
                    .build()

            ibcB.sendPacket(packet)

            ibcA.recvPacketOrdered(MsgRecvPacket.newBuilder()
                    .setPacket(packet)
                    .setProofCommitment(ibcB.chanProof(chanBid).toByteString())
                    .setProofHeight(ibcB.host().getCurrentHeight())
                    .build())

            val ack = ibcA.chan(chanAid).acknowledgements[sequence]!!.toJson()

            ibcB.acknowledgePacketOrdered(MsgAcknowledgement.newBuilder()
                    .setPacket(packet)
                    .setAcknowledgement(ack)
                    .setProofAcked(ibcA.chanProof(chanAid).toByteString())
                    .setProofHeight(ibcA.host().getCurrentHeight())
                    .build())
        }

        ibcA.chanCloseInit(MsgChannelCloseInit.newBuilder().apply{
            portId = portAid.id
            channelId = chanAid.id
        }.build())

        ibcB.chanCloseConfirm(MsgChannelCloseConfirm.newBuilder().apply{
            portId = portBid.id
            channelId = chanBid.id
            proofInit = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())
    }

    @Test
    fun `fund allocation`() {
        val a1 = network.createNode()
        val a2 = network.createNode()
        val b1 = network.createNode()
        val b2 = network.createNode()

        val ibcA = TestCordaIbcClient(network, a1)
        val ibcB = TestCordaIbcClient(network, b1)

        ibcA.createGenesis(listOf(a1.party, a2.party))
        ibcA.createHost()
        ibcA.createBank()

        ibcB.createGenesis(listOf(b1.party, b2.party))
        ibcB.createHost()
        ibcB.createBank()

        val JPY = Denom.fromString("JPY")
        val USD = Denom.fromString("USD")

        ibcA.allocateFund(a1.addr, JPY, Amount.fromLong(10000))
        ibcA.allocateFund(a2.addr, JPY, Amount.fromLong(20000))

        ibcB.allocateFund(b1.addr, USD, Amount.fromLong(100))
        ibcB.allocateFund(b2.addr, USD, Amount.fromLong(200))

        val bankA = ibcA.bank()
        val bankB = ibcB.bank()

        assert(bankA.allocated[JPY]!![a1.addr]!! == Amount.fromLong(10000))
        assert(bankA.allocated[JPY]!![a2.addr]!! == Amount.fromLong(20000))
        assert(bankA.allocated[JPY]!![b1.addr] == null)
        assert(bankA.allocated[JPY]!![b2.addr] == null)
        assert(bankA.allocated[USD] == null)

        assert(bankB.allocated[USD]!![b1.addr]!! == Amount.fromLong(100))
        assert(bankB.allocated[USD]!![b2.addr]!! == Amount.fromLong(200))
        assert(bankB.allocated[USD]!![a1.addr] == null)
        assert(bankB.allocated[USD]!![a2.addr] == null)
        assert(bankB.allocated[JPY] == null)
    }

    @Test
    fun `ics-20`() {
        val a1 = network.createNode()
        val a2 = network.createNode()
        val b1 = network.createNode()
        val b2 = network.createNode()

        val ibcA = TestCordaIbcClient(network, a1)
        val ibcB = TestCordaIbcClient(network, b1)

        // create host&bank for chain A
        ibcA.createGenesis(listOf(a1.party, a2.party))
        ibcA.createHost()
        ibcA.createBank()

        // create host&bank for chain B
        ibcB.createGenesis(listOf(b1.party, b2.party))
        ibcB.createHost()
        ibcB.createBank()

        // denominations for this test case
        val JPY = Denom.fromString("JPY")
        val USD = Denom.fromString("USD")

        // allocate some JPYs for chain A
        ibcA.allocateFund(a1.addr, JPY, Amount.fromLong(10000))
        ibcA.allocateFund(a2.addr, JPY, Amount.fromLong(20000))

        // allocate some USDs for chain B
        ibcB.allocateFund(b1.addr, USD, Amount.fromLong(1000))
        ibcB.allocateFund(b2.addr, USD, Amount.fromLong(2000))

        // create clients on both chains
        val clientAid = ibcA.createClient(MsgCreateClient.newBuilder().apply {
            clientState = Corda.ClientState.newBuilder().apply{
                baseId = ibcB.host().baseId.toProto()
                notaryKey = ibcB.host().notary.owningKey.toProto()
            }.build().pack()
            consensusState = ibcB.host().getConsensusState(HEIGHT).anyConsensusState
        }.build())

        val clientBid = ibcB.createClient(MsgCreateClient.newBuilder().apply {
            clientState = Corda.ClientState.newBuilder().apply{
                baseId = ibcA.host().baseId.toProto()
                notaryKey = ibcA.host().notary.owningKey.toProto()
            }.build().pack()
            consensusState = ibcA.host().getConsensusState(HEIGHT).anyConsensusState
        }.build())

        // create a connection between chain A and chain B
        val connAid = ibcA.connOpenInit(MsgConnectionOpenInit.newBuilder().apply{
            clientId = clientAid.id
            counterpartyBuilder.clientId = clientBid.id
            counterpartyBuilder.prefix = ibcB.host().getCommitmentPrefix()
            version = ibcA.host().getCompatibleVersions().single()
            delayPeriod = 0
        }.build())

        val connBid = ibcB.connOpenTry(MsgConnectionOpenTry.newBuilder().apply{
            clientId = clientBid.id
            previousConnectionId = ""
            clientState = ibcA.client(clientAid).anyClientState
            counterpartyBuilder.clientId = clientAid.id
            counterpartyBuilder.connectionId = connAid.id
            counterpartyBuilder.prefix = ibcA.host().getCommitmentPrefix()
            delayPeriod = 0
            addAllCounterpartyVersions(ibcA.host().getCompatibleVersions())
            proofHeight = ibcA.host().getCurrentHeight()
            proofInit = ibcA.connProof(connAid).toByteString()
            proofClient = ibcA.clientProof(clientAid).toByteString()
            proofConsensus = ibcA.clientProof(clientAid).toByteString()
            consensusHeight = ibcB.host().getCurrentHeight()
        }.build())

        ibcA.connOpenAck(MsgConnectionOpenAck.newBuilder().apply{
            connectionId = connAid.id
            counterpartyConnectionId = connBid.id
            version = ibcB.conn(connBid).end.versionsList.single()
            clientState = ibcB.client(clientBid).anyClientState
            proofHeight = ibcB.host().getCurrentHeight()
            proofTry = ibcB.connProof(connBid).toByteString()
            proofClient = ibcB.clientProof(clientBid).toByteString()
            proofConsensus = ibcB.clientProof(clientBid).toByteString()
            consensusHeight = ibcA.host().getCurrentHeight()
        }.build())

        ibcB.connOpenConfirm(MsgConnectionOpenConfirm.newBuilder().apply{
            connectionId = connBid.id
            proofAck = ibcA.connProof(connAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        // create a channel between chain A and chain B
        val portTransferId = Identifier("transfer-old")
        val channelVersionA = "CHANNEL_VERSION_A" // arbitrary string is ok
        val channelVersionB = "CHANNEL_VERSION_B" // arbitrary string is ok
        val ordering = ChannelOuterClass.Order.ORDER_UNORDERED

        val chanAid = ibcA.chanOpenInit(MsgChannelOpenInit.newBuilder().apply{
            portId = portTransferId.id
            channelBuilder.state = ChannelOuterClass.State.STATE_UNINITIALIZED_UNSPECIFIED
            channelBuilder.ordering = ordering
            channelBuilder.counterpartyBuilder.portId = portTransferId.id
            channelBuilder.counterpartyBuilder.channelId = ""
            channelBuilder.addAllConnectionHops(listOf(connAid.id))
            channelBuilder.version = channelVersionA
        }.build())

        val chanBid = ibcB.chanOpenTry(MsgChannelOpenTry.newBuilder().apply{
            portId = portTransferId.id
            previousChannelId = ""
            channelBuilder.state = ChannelOuterClass.State.STATE_UNINITIALIZED_UNSPECIFIED
            channelBuilder.ordering = ordering
            channelBuilder.counterpartyBuilder.portId = portTransferId.id
            channelBuilder.counterpartyBuilder.channelId = chanAid.id
            channelBuilder.addAllConnectionHops(listOf(connBid.id))
            channelBuilder.version = channelVersionB
            counterpartyVersion = ibcA.chan(chanAid).end.version
            proofInit = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        ibcA.chanOpenAck(MsgChannelOpenAck.newBuilder().apply{
            portId = portTransferId.id
            channelId = chanAid.id
            counterpartyChannelId = chanBid.id
            counterpartyVersion = ibcB.chan(chanBid).end.version
            proofTry = ibcB.chanProof(chanBid).toByteString()
            proofHeight = ibcB.host().getCurrentHeight()
        }.build())

        ibcB.chanOpenConfirm(MsgChannelOpenConfirm.newBuilder().apply{
            portId = portTransferId.id
            channelId = chanBid.id
            proofAck = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        // A1 sends 100 JPY to B2
        val seqA1toB2 = ibcA.chan(chanAid).nextSequenceSend
        ibcA.sendTransfer(MsgTransfer.newBuilder().apply{
            sourcePort = portTransferId.id
            sourceChannel = chanAid.id
            tokenBuilder.denom = JPY.toString()
            tokenBuilder.amount = "100"
            sender = a1.addr.toBech32()
            receiver = b2.addr.toBech32()
            timeoutHeight = Height.getDefaultInstance()!!
            timeoutTimestamp = 0
        }.build())

        // A2 sends 100 JPY to B1
        val seqA2toB1 = ibcA.chan(chanAid).nextSequenceSend
        ibcA.sendTransfer(MsgTransfer.newBuilder().apply{
            sourcePort = portTransferId.id
            sourceChannel = chanAid.id
            tokenBuilder.denom = JPY.toString()
            tokenBuilder.amount = "100"
            sender = a2.addr.toBech32()
            receiver = b1.addr.toBech32()
            timeoutHeight = Height.getDefaultInstance()!!
            timeoutTimestamp = 0
        }.build())

        // B1 receives 100 JPY from A2
        val packetA2toB1 = ibcA.chan(chanAid).packets[seqA2toB1]!!
        ibcB.recvPacketUnordered(MsgRecvPacket.newBuilder().apply{
            packet = packetA2toB1
            proofCommitment = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        // A2 receives ack from B1
        val ackB1toA2 = ibcB.chan(chanBid).acknowledgements[seqA2toB1]!!
        ibcA.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder().apply{
            packet = packetA2toB1
            acknowledgement = ackB1toA2.toJson()
            proofAcked = ibcB.chanProof(chanBid).toByteString()
            proofHeight = ibcB.host().getCurrentHeight()
        }.build())

        // B2 receives 100 JPY from A1
        val packetA1toB2 = ibcA.chan(chanAid).packets[seqA1toB2]!!
        ibcB.recvPacketUnordered(MsgRecvPacket.newBuilder().apply{
            packet = packetA1toB2
            proofCommitment = ibcA.chanProof(chanAid).toByteString()
            proofHeight = ibcA.host().getCurrentHeight()
        }.build())

        // A1 receives ack from B2
        val ackB2toA1 = ibcB.chan(chanBid).acknowledgements[seqA1toB2]!!
        ibcA.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder().apply{
            packet = packetA1toB2
            acknowledgement = ackB2toA1.toJson()
            proofAcked = ibcB.chanProof(chanBid).toByteString()
            proofHeight = ibcB.host().getCurrentHeight()
        }.build())

        assert(ackB1toA2.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)
        assert(ackB2toA1.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)

        // sending back
        for (i in 0 until 2) {
            val seqB1toA1 = ibcB.chan(chanBid).nextSequenceSend
            ibcB.sendTransfer(MsgTransfer.newBuilder().apply{
                sourcePort = portTransferId.id
                sourceChannel = chanBid.id
                tokenBuilder.denom = "${portTransferId.id}/${chanBid.id}/JPY"
                tokenBuilder.amount = "50"
                sender = b1.addr.toBech32()
                receiver = a1.addr.toBech32()
                timeoutHeight = Height.getDefaultInstance()!!
                timeoutTimestamp = 0
            }.build())

            val packetB1toA1 = ibcB.chan(chanBid).packets[seqB1toA1]!!
            ibcA.recvPacketUnordered(MsgRecvPacket.newBuilder().apply{
                packet = packetB1toA1
                proofCommitment = ibcB.chanProof(chanBid).toByteString()
                proofHeight = ibcB.host().getCurrentHeight()
            }.build())

            val ackA1toB1 = ibcA.chan(chanAid).acknowledgements[seqB1toA1]!!
            ibcB.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder().apply{
                packet = packetB1toA1
                acknowledgement = ackA1toB1.toJson()
                proofAcked = ibcA.chanProof(chanAid).toByteString()
                proofHeight = ibcA.host().getCurrentHeight()
            }.build())
        }

        assertFailsWith<ExecutionException> {
            ibcB.sendTransfer(MsgTransfer.newBuilder().apply{
                sourcePort = portTransferId.id
                sourceChannel = chanBid.id
                tokenBuilder.denom = "${portTransferId.id}/${chanBid.id}/JPY"
                tokenBuilder.amount = "1"
                sender = b1.addr.toBech32()
                receiver = a1.addr.toBech32()
                timeoutHeight = Height.getDefaultInstance()!!
                timeoutTimestamp = 0
            }.build())
        }
    }

    @Test
    fun `ics-20 with Cash`() {
        // nodes
        val alice = network.createPartyNode(ALICE_NAME)
        val bankA = network.createPartyNode(DUMMY_BANK_A_NAME)
        val bob = network.createPartyNode(BOB_NAME)
        val bankB = network.createPartyNode(DUMMY_BANK_B_NAME)

        // clients for nodes
        val ibcAlice = TestCordaIbcClient(network, alice)
        val ibcBankA = TestCordaIbcClient(network, bankA)
        val ibcBob = TestCordaIbcClient(network, bob)
        val ibcBankB = TestCordaIbcClient(network, bankB)

        // create host&bank for chain A
        ibcAlice.createGenesis(listOf(alice.party, bankA.party))
        ibcAlice.createHost()
        ibcBankA.setBaseId(ibcAlice.baseId)
        ibcBankA.createCashBank(bankA.party)

        // create host&bank for chain B
        ibcBob.createGenesis(listOf(bob.party, bankB.party))
        ibcBob.createHost()
        ibcBankB.setBaseId(ibcBob.baseId)
        ibcBankB.createCashBank(bankB.party)

        // denomination for this test case
        val JPY = Denom.fromIssuedCurrency(bankA.addr.toPublicKey(), Currency.getInstance("JPY"))

        // allocate some JPYs for Alice on chain A
        ibcBankA.allocateCash(alice.party, 1000, JPY.currency)

        // create clients on both chains
        val clientAid = ibcAlice.createClient(MsgCreateClient.newBuilder().apply {
            clientState = Corda.ClientState.newBuilder().apply{
                baseId = ibcBob.host().baseId.toProto()
                notaryKey = ibcBob.host().notary.owningKey.toProto()
            }.build().pack()
            consensusState = ibcBob.host().getConsensusState(HEIGHT).anyConsensusState
        }.build())

        val clientBid = ibcBob.createClient(MsgCreateClient.newBuilder().apply {
            clientState = Corda.ClientState.newBuilder().apply{
                baseId = ibcAlice.host().baseId.toProto()
                notaryKey = ibcAlice.host().notary.owningKey.toProto()
            }.build().pack()
            consensusState = ibcAlice.host().getConsensusState(HEIGHT).anyConsensusState
        }.build())

        // create a connection between chain A and chain B
        val connAid = ibcAlice.connOpenInit(MsgConnectionOpenInit.newBuilder().apply{
            clientId = clientAid.id
            counterpartyBuilder.clientId = clientBid.id
            counterpartyBuilder.prefix = ibcBob.host().getCommitmentPrefix()
            version = ibcAlice.host().getCompatibleVersions().single()
            delayPeriod = 0
        }.build())

        val connBid = ibcBob.connOpenTry(MsgConnectionOpenTry.newBuilder().apply{
            clientId = clientBid.id
            previousConnectionId = ""
            clientState = ibcAlice.client(clientAid).anyClientState
            counterpartyBuilder.clientId = clientAid.id
            counterpartyBuilder.connectionId = connAid.id
            counterpartyBuilder.prefix = ibcAlice.host().getCommitmentPrefix()
            delayPeriod = 0
            addAllCounterpartyVersions(ibcAlice.host().getCompatibleVersions())
            proofHeight = ibcAlice.host().getCurrentHeight()
            proofInit = ibcAlice.connProof(connAid).toByteString()
            proofClient = ibcAlice.clientProof(clientAid).toByteString()
            proofConsensus = ibcAlice.clientProof(clientAid).toByteString()
            consensusHeight = ibcBob.host().getCurrentHeight()
        }.build())

        ibcAlice.connOpenAck(MsgConnectionOpenAck.newBuilder().apply{
            connectionId = connAid.id
            counterpartyConnectionId = connBid.id
            version = ibcBob.conn(connBid).end.versionsList.single()
            clientState = ibcBob.client(clientBid).anyClientState
            proofHeight = ibcBob.host().getCurrentHeight()
            proofTry = ibcBob.connProof(connBid).toByteString()
            proofClient = ibcBob.clientProof(clientBid).toByteString()
            proofConsensus = ibcBob.clientProof(clientBid).toByteString()
            consensusHeight = ibcAlice.host().getCurrentHeight()
        }.build())

        ibcBob.connOpenConfirm(MsgConnectionOpenConfirm.newBuilder().apply{
            connectionId = connBid.id
            proofAck = ibcAlice.connProof(connAid).toByteString()
            proofHeight = ibcAlice.host().getCurrentHeight()
        }.build())

        // create a channel between chain A and chain B
        val portTransferId = Identifier("transfer")
        val channelVersionA = "CHANNEL_VERSION_A" // arbitrary string is ok
        val channelVersionB = "CHANNEL_VERSION_B" // arbitrary string is ok
        val ordering = ChannelOuterClass.Order.ORDER_UNORDERED

        val chanAid = ibcAlice.chanOpenInit(MsgChannelOpenInit.newBuilder().apply{
            portId = portTransferId.id
            channelBuilder.state = ChannelOuterClass.State.STATE_UNINITIALIZED_UNSPECIFIED
            channelBuilder.ordering = ordering
            channelBuilder.counterpartyBuilder.portId = portTransferId.id
            channelBuilder.counterpartyBuilder.channelId = ""
            channelBuilder.addAllConnectionHops(listOf(connAid.id))
            channelBuilder.version = channelVersionA
        }.build())

        val chanBid = ibcBob.chanOpenTry(MsgChannelOpenTry.newBuilder().apply{
            portId = portTransferId.id
            previousChannelId = ""
            channelBuilder.state = ChannelOuterClass.State.STATE_UNINITIALIZED_UNSPECIFIED
            channelBuilder.ordering = ordering
            channelBuilder.counterpartyBuilder.portId = portTransferId.id
            channelBuilder.counterpartyBuilder.channelId = chanAid.id
            channelBuilder.addAllConnectionHops(listOf(connBid.id))
            channelBuilder.version = channelVersionB
            counterpartyVersion = ibcAlice.chan(chanAid).end.version
            proofInit = ibcAlice.chanProof(chanAid).toByteString()
            proofHeight = ibcAlice.host().getCurrentHeight()
        }.build())

        ibcAlice.chanOpenAck(MsgChannelOpenAck.newBuilder().apply{
            portId = portTransferId.id
            channelId = chanAid.id
            counterpartyChannelId = chanBid.id
            counterpartyVersion = ibcBob.chan(chanBid).end.version
            proofTry = ibcBob.chanProof(chanBid).toByteString()
            proofHeight = ibcBob.host().getCurrentHeight()
        }.build())

        ibcBob.chanOpenConfirm(MsgChannelOpenConfirm.newBuilder().apply{
            portId = portTransferId.id
            channelId = chanBid.id
            proofAck = ibcAlice.chanProof(chanAid).toByteString()
            proofHeight = ibcAlice.host().getCurrentHeight()
        }.build())

        // Alice sends 100 JPY to Bob
        val seqAliceToBob = ibcAlice.chan(chanAid).nextSequenceSend
        ibcAlice.transfer(MsgTransfer.newBuilder().apply{
            sourcePort = portTransferId.id
            sourceChannel = chanAid.id
            tokenBuilder.denom = JPY.toString()
            tokenBuilder.amount = "100"
            sender = alice.addr.toBech32()
            receiver = bob.addr.toBech32()
            timeoutHeight = Height.getDefaultInstance()!!
            timeoutTimestamp = 0
        }.build())

        // Bob receives 100 JPY from Alice
        val packetAliceToBob = ibcAlice.chan(chanAid).packets[seqAliceToBob]!!
        ibcBob.recvPacketUnordered(MsgRecvPacket.newBuilder().apply{
            packet = packetAliceToBob
            proofCommitment = ibcAlice.chanProof(chanAid).toByteString()
            proofHeight = ibcAlice.host().getCurrentHeight()
        }.build())

        // Alice receives ack from Bob
        val ackBobToAlice = ibcBob.chan(chanBid).acknowledgements[seqAliceToBob]!!
        assert(ackBobToAlice.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)
        ibcAlice.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder().apply{
            packet = packetAliceToBob
            acknowledgement = ackBobToAlice.toJson()
            proofAcked = ibcBob.chanProof(chanBid).toByteString()
            proofHeight = ibcBob.host().getCurrentHeight()
        }.build())

        // sending back
        for (i in 0 until 2) {
            val seqBobToAlice = ibcBob.chan(chanBid).nextSequenceSend
            ibcBob.transfer(MsgTransfer.newBuilder().apply{
                sourcePort = portTransferId.id
                sourceChannel = chanBid.id
                tokenBuilder.denom = JPY.addPath(portTransferId, chanBid).toIbcDenom()
                tokenBuilder.amount = "50"
                sender = bob.addr.toBech32()
                receiver = alice.addr.toBech32()
                timeoutHeight = Height.getDefaultInstance()!!
                timeoutTimestamp = 0
            }.build())

            val packetBobToAlice = ibcBob.chan(chanBid).packets[seqBobToAlice]!!
            ibcBankA.recvPacketUnordered(MsgRecvPacket.newBuilder().apply{
                packet = packetBobToAlice
                proofCommitment = ibcBob.chanProof(chanBid).toByteString()
                proofHeight = ibcBob.host().getCurrentHeight()
            }.build())

            val ackAliceToBob = ibcAlice.chan(chanAid).acknowledgements[seqBobToAlice]!!
            assert(ackAliceToBob.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)
            ibcBob.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder().apply{
                packet = packetBobToAlice
                acknowledgement = ackAliceToBob.toJson()
                proofAcked = ibcAlice.chanProof(chanAid).toByteString()
                proofHeight = ibcAlice.host().getCurrentHeight()
            }.build())
        }

        assertFailsWith<ExecutionException> {
            ibcBob.transfer(MsgTransfer.newBuilder().apply{
                sourcePort = portTransferId.id
                sourceChannel = chanBid.id
                tokenBuilder.denom = JPY.addPath(portTransferId, chanBid).toIbcDenom()
                tokenBuilder.amount = "1"
                sender = bob.addr.toBech32()
                receiver = alice.addr.toBech32()
                timeoutHeight = Height.getDefaultInstance()!!
                timeoutTimestamp = 0
            }.build())
        }
    }
}
