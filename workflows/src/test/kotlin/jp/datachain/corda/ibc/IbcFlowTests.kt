package jp.datachain.corda.ibc

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.channel.v1.Tx.*
import ibc.core.client.v1.Client.Height
import ibc.core.client.v1.Tx.*
import ibc.core.connection.v1.Tx.*
import ibc.lightclients.corda.v1.Corda
import jp.datachain.corda.ibc.clients.corda.HEIGHT
import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.flows.ics2.IbcClientCreateResponderFlow
import jp.datachain.corda.ibc.flows.ics20.*
import jp.datachain.corda.ibc.flows.ics24.*
import jp.datachain.corda.ibc.flows.ics3.*
import jp.datachain.corda.ibc.flows.ics4.*
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.toJson
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException
import kotlin.test.assertFailsWith

class IbcFlowTests {
    private val networkParam = MockNetworkParameters(
            cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("jp.datachain.corda.ibc.contracts"),
                    TestCordapp.findCordapp("jp.datachain.corda.ibc.flows")
            ),
            notarySpecs = listOf(
                    MockNetworkNotarySpec(CordaX500Name("My Mock Notary Service", "Kawasaki", "JP"), validating = true)
            )
    )
    private val network = MockNetwork(networkParam.withNetworkParameters(networkParam.networkParameters.copy(minimumPlatformVersion = 4)))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()
    private val x = network.createNode()
    private val y = network.createNode()
    private val z = network.createNode()

    init {
        listOf(a, b, c, x, y, z).forEach {
            // Host management
            it.registerInitiatedFlow(IbcGenesisCreateResponderFlow::class.java)
            it.registerInitiatedFlow(IbcHostCreateResponderFlow::class.java)

            // Client management
            it.registerInitiatedFlow(IbcClientCreateResponderFlow::class.java)

            // Connection handshake
            it.registerInitiatedFlow(IbcConnOpenInitResponderFlow::class.java)
            it.registerInitiatedFlow(IbcConnOpenTryResponderFlow::class.java)
            it.registerInitiatedFlow(IbcConnOpenAckResponderFlow::class.java)
            it.registerInitiatedFlow(IbcConnOpenConfirmResponderFlow::class.java)

            // Channel handshake
            it.registerInitiatedFlow(IbcChanOpenInitResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanOpenTryResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanOpenAckResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanOpenConfirmResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanCloseInitResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanCloseConfirmResponderFlow::class.java)

            // Packet transportation
            it.registerInitiatedFlow(IbcSendPacketResponderFlow::class.java)
            it.registerInitiatedFlow(IbcRecvPacketResponderFlow::class.java)
            it.registerInitiatedFlow(IbcAcknowledgePacketResponderFlow::class.java)

            // ics20
            it.registerInitiatedFlow(IbcBankCreateResponderFlow::class.java)
            it.registerInitiatedFlow(IbcFundAllocateResponderFlow::class.java)
            it.registerInitiatedFlow(IbcSendTransferResponderFlow::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `relayer logic`() {
        val ibcA = TestCordaIbcClient(network, a)
        val ibcB = TestCordaIbcClient(network, x)

        ibcA.createGenesis(listOf(
                a.info.legalIdentities.single(),
                b.info.legalIdentities.single(),
                c.info.legalIdentities.single()
        ))
        ibcA.createHost()

        ibcB.createGenesis(listOf(
                x.info.legalIdentities.single(),
                y.info.legalIdentities.single(),
                z.info.legalIdentities.single()
        ))
        ibcB.createHost()

        val expectedClientId = "corda-ibc-0"

        val clientAid = ibcA.createClient(MsgCreateClient.newBuilder().apply{
            clientState = Any.pack(Corda.ClientState.newBuilder().setId(expectedClientId).build(), "")
            consensusState = ibcB.host().getConsensusState(HEIGHT).consensusState
        }.build())
        assert(clientAid.id == expectedClientId)

        val clientBid = ibcB.createClient(MsgCreateClient.newBuilder().apply{
            clientState = Any.pack(Corda.ClientState.newBuilder().setId(expectedClientId).build(), "")
            consensusState = ibcA.host().getConsensusState(HEIGHT).consensusState
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
            clientState = ibcA.client(clientAid).clientState
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
            clientState = ibcB.client(clientBid).clientState
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
        val ordering = ChannelOuterClass.Order.ORDER_UNORDERED

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

            ibcB.recvPacketUnordered(MsgRecvPacket.newBuilder()
                    .setPacket(packet)
                    .setProofCommitment(ibcA.chanProof(chanAid).toByteString())
                    .setProofHeight(ibcA.host().getCurrentHeight())
                    .build())

            val ack = ibcB.chan(chanBid).acknowledgements[sequence]!!.toJson()

            ibcA.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder()
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

            ibcA.recvPacketUnordered(MsgRecvPacket.newBuilder()
                    .setPacket(packet)
                    .setProofCommitment(ibcB.chanProof(chanBid).toByteString())
                    .setProofHeight(ibcB.host().getCurrentHeight())
                    .build())

            val ack = ibcA.chan(chanAid).acknowledgements[sequence]!!.toJson()

            ibcB.acknowledgePacketUnordered(MsgAcknowledgement.newBuilder()
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

    /*
    @Test
    fun `fund allocation`() {
        val ibcA = TestCordaIbcClient(network, a)
        val ibcB = TestCordaIbcClient(network, b)
        val ibcC = TestCordaIbcClient(network, c)
        val ibcX = TestCordaIbcClient(network, x)
        val ibcY = TestCordaIbcClient(network, y)
        val ibcZ = TestCordaIbcClient(network, z)

        val aKey = a.info.legalIdentities.single().owningKey
        val bKey = b.info.legalIdentities.single().owningKey
        val cKey = c.info.legalIdentities.single().owningKey
        val xKey = x.info.legalIdentities.single().owningKey
        val yKey = y.info.legalIdentities.single().owningKey
        val zKey = z.info.legalIdentities.single().owningKey

        ibcA.createHostAndBank(listOf(
                a.info.legalIdentities.single(),
                b.info.legalIdentities.single(),
                c.info.legalIdentities.single()
        ))
        ibcX.createHostAndBank(listOf(
                x.info.legalIdentities.single(),
                y.info.legalIdentities.single(),
                z.info.legalIdentities.single()
        ))
        ibcB._baseId = ibcA.baseId
        ibcC._baseId = ibcA.baseId
        ibcY._baseId = ibcX.baseId
        ibcZ._baseId = ibcX.baseId

        ibcB.allocateFund(bKey, Denom("JPY"), Amount(2000))
        ibcC.allocateFund(cKey, Denom("JPY"), Amount(3000))
        ibcA.allocateFund(aKey, Denom("JPY"), Amount(11000))
        ibcA.allocateFund(bKey, Denom("JPY"), Amount(20000))
        ibcA.allocateFund(cKey, Denom("JPY"), Amount(30000))

        ibcY.allocateFund(yKey, Denom("USD"), Amount(100))
        ibcZ.allocateFund(zKey, Denom("USD"), Amount(200))
        ibcX.allocateFund(yKey, Denom("USD"), Amount(1000))
        ibcX.allocateFund(zKey, Denom("USD"), Amount(2000))

        val bankABC = ibcA.bank()
        val bankXYZ = ibcX.bank()

        assert(bankABC.allocated[Denom("JPY")]!![aKey]!! == Amount(11000))
        assert(bankABC.allocated[Denom("JPY")]!![bKey]!! == Amount(22000))
        assert(bankABC.allocated[Denom("JPY")]!![cKey]!! == Amount(33000))
        assert(bankABC.allocated[Denom("USD")] == null)

        assert(bankXYZ.allocated[Denom("USD")]!![xKey] == null)
        assert(bankXYZ.allocated[Denom("USD")]!![yKey]!! == Amount(1100))
        assert(bankXYZ.allocated[Denom("USD")]!![zKey]!! == Amount(2200))
        assert(bankXYZ.allocated[Denom("JPY")] == null)
    }

    @Test
    fun `ics-20`() {
        val ibcA = TestCordaIbcClient(network, a)
        val ibcB = TestCordaIbcClient(network, b)
        val ibcC = TestCordaIbcClient(network, c)
        val ibcX = TestCordaIbcClient(network, x)
        val ibcY = TestCordaIbcClient(network, y)
        val ibcZ = TestCordaIbcClient(network, z)

        val aKey = a.info.legalIdentities.single().owningKey
        val bKey = b.info.legalIdentities.single().owningKey
        val cKey = c.info.legalIdentities.single().owningKey
        val xKey = x.info.legalIdentities.single().owningKey
        val yKey = y.info.legalIdentities.single().owningKey
        val zKey = z.info.legalIdentities.single().owningKey

        // create host&bank for the ABC group
        ibcA.createHostAndBank(listOf(
                a.info.legalIdentities.single(),
                b.info.legalIdentities.single(),
                c.info.legalIdentities.single()
        ))
        // create host&bank for the XYZ group
        ibcX.createHostAndBank(listOf(
                x.info.legalIdentities.single(),
                y.info.legalIdentities.single(),
                z.info.legalIdentities.single()
        ))
        // A, B and C share the same baseID
        ibcB._baseId = ibcA.baseId
        ibcC._baseId = ibcA.baseId
        // X, Y and Z share the same baseID
        ibcY._baseId = ibcX.baseId
        ibcZ._baseId = ibcX.baseId

        // allocate some JPYs for A, B and C
        ibcC.allocateFund(aKey, Denom("JPY"), Amount(1000))
        ibcC.allocateFund(bKey, Denom("JPY"), Amount(2000))
        ibcC.allocateFund(cKey, Denom("JPY"), Amount(3000))
        // allocate some USDs for X, Y and Z
        ibcZ.allocateFund(xKey, Denom("USD"), Amount(10000))
        ibcZ.allocateFund(yKey, Denom("USD"), Amount(20000))
        ibcZ.allocateFund(zKey, Denom("USD"), Amount(30000))

        val idCliABC = Identifier("clientABC")
        val idCliXYZ = Identifier("clientXYZ")

        // ABC creates client for interacting with XYZ
        ibcA.createClient(
                idCliABC,
                ClientType.CordaClient,
                ibcY.host().getConsensusState(HEIGHT))
        // XYZ creates client for interacting with ABC
        ibcX.createClient(
                idCliXYZ,
                ClientType.CordaClient,
                ibcB.host().getConsensusState(HEIGHT))

        val idConnABC = Identifier("connABC")
        val idConnXYZ = Identifier("connXYZ")

        // ABC executes connOpenInit
        ibcB.connOpenInit(
                idConnABC,
                idConnXYZ,
                ibcZ.host().getCommitmentPrefix(),
                idCliABC,
                idCliXYZ,
                null)
        // XYZ executes connOpenTry
        ibcY.connOpenTry(
                idConnXYZ,
                idConnXYZ,
                idConnABC,
                ibcC.host().getCommitmentPrefix(),
                idCliABC,
                idCliXYZ,
                ibcC.host().getCompatibleVersions(),
                ibcC.connProof(idConnABC),
                ibcC.clientProof(idCliABC),
                ibcC.host().getCurrentHeight(),
                ibcC.host().getCurrentHeight())
        // ABC executes connOpenAck
        ibcC.connOpenAck(
                idConnABC,
                ibcX.conn(idConnXYZ).end.versionsList.single(),
                idConnXYZ,
                ibcX.connProof(idConnXYZ),
                ibcX.clientProof(idCliXYZ),
                ibcX.host().getCurrentHeight(),
                ibcX.host().getCurrentHeight())
        // XYZ executes connOpenConfirm
        ibcZ.connOpenConfirm(
                idConnXYZ,
                ibcA.connProof(idConnABC),
                ibcA.host().getCurrentHeight())

        val idPortABC = Identifier("transfer")
        val idPortXYZ = Identifier("transfer")
        val idChanABC = Identifier("chanABC")
        val idChanXYZ = Identifier("chanXYZ")
        val order = ChannelOuterClass.Order.ORDER_UNORDERED

        // ABC executes chanOpenInit
        ibcA.chanOpenInit(
                order,
                listOf(idConnABC),
                idPortABC,
                idChanABC,
                idPortXYZ,
                idChanXYZ,
                ibcB.conn(idConnABC).end.versionsList.single().toString())
        // XYZ executes chanOpenTry
        ibcX.chanOpenTry(
                order,
                listOf(idConnXYZ),
                idPortXYZ,
                idChanXYZ,
                idChanXYZ,
                idPortABC,
                idChanABC,
                ibcY.conn(idConnXYZ).end.versionsList.single().toString(),
                ibcB.chan(idChanABC).end.version,
                ibcB.chanProof(idChanABC),
                ibcB.host().getCurrentHeight())
        // ABC executes chanOpenAck
        ibcB.chanOpenAck(
                idPortABC,
                idChanABC,
                ibcZ.chan(idChanXYZ).end.version,
                idChanXYZ,
                ibcZ.chanProof(idChanXYZ),
                ibcZ.host().getCurrentHeight())
        // XYZ executes chanOpenConfirm
        ibcY.chanOpenConfirm(
                idPortXYZ,
                idChanXYZ,
                ibcC.chanProof(idChanABC),
                ibcC.host().getCurrentHeight())

        val denom = Denom("JPY")
        val amount = Amount(100)
        val sourcePort = idPortABC
        val sourceChannel = idChanABC
        val timeoutHeight = Height.getDefaultInstance()
        val timeoutTimestamp = Timestamp(0)
        // C sends 100 JPY to Z
        val seqCtoZ = ibcC.chan(idChanABC).nextSequenceSend
        ibcC.sendTransfer(
                sourcePort,
                sourceChannel,
                denom,
                amount,
                cKey,
                zKey,
                timeoutHeight,
                timeoutTimestamp)
        // A sends 100 JPY to X
        val seqAtoX = ibcC.chan(idChanABC).nextSequenceSend
        ibcA.sendTransfer(
                sourcePort,
                sourceChannel,
                denom,
                amount,
                aKey,
                xKey,
                timeoutHeight,
                timeoutTimestamp)
        // B sends 100 JPY to Y
        val seqBtoY = ibcC.chan(idChanABC).nextSequenceSend
        ibcB.sendTransfer(
                sourcePort,
                sourceChannel,
                denom,
                amount,
                bKey,
                yKey,
                timeoutHeight,
                timeoutTimestamp)

        // Z receives 100 JPY from C
        val packetCtoZ = ibcC.chan(idChanABC).packets[seqCtoZ]!!
        ibcZ.recvPacketUnordered(
                packetCtoZ,
                ibcC.chanProof(idChanABC),
                ibcC.host().getCurrentHeight())
        // C receives ack from Z
        val ackZtoC = ibcZ.chan(idChanXYZ).acknowledgements[seqCtoZ]!!
        ibcC.acknowledgePacketUnordered(
                packetCtoZ,
                ackZtoC,
                ibcZ.chanProof(idChanXYZ),
                ibcZ.host().getCurrentHeight())
        // X receives 100 JPY from A
        val packetAtoX = ibcA.chan(idChanABC).packets[seqAtoX]!!
        ibcX.recvPacketUnordered(
                packetAtoX,
                ibcA.chanProof(idChanABC),
                ibcA.host().getCurrentHeight())
        // A receives ack from X
        val ackXtoA = ibcX.chan(idChanXYZ).acknowledgements[seqAtoX]!!
        ibcA.acknowledgePacketUnordered(
                packetAtoX,
                ackXtoA,
                ibcX.chanProof(idChanXYZ),
                ibcX.host().getCurrentHeight())
        // Y receives 100 JPY from B
        val packetBtoY = ibcB.chan(idChanABC).packets[seqBtoY]!!
        ibcY.recvPacketUnordered(
                packetBtoY,
                ibcB.chanProof(idChanABC),
                ibcB.host().getCurrentHeight())
        // B receives ack from Y
        val ackYtoB = ibcY.chan(idChanXYZ).acknowledgements[seqBtoY]!!
        ibcB.acknowledgePacketUnordered(
                packetBtoY,
                ackYtoB,
                ibcY.chanProof(idChanXYZ),
                ibcY.host().getCurrentHeight())

        assert(ackXtoA.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)
        assert(ackYtoB.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)
        assert(ackZtoC.responseCase == ChannelOuterClass.Acknowledgement.ResponseCase.RESULT)

        // sending back
        for (i in 0 until 2) {
            val seqXtoA = ibcX.chan(idChanXYZ).nextSequenceSend
            ibcX.sendTransfer(
                    idPortXYZ,
                    idChanXYZ,
                    Denom("${idPortXYZ.id}/${idChanXYZ.id}/JPY"),
                    Amount(50),
                    xKey,
                    aKey,
                    Height.getDefaultInstance(),
                    Timestamp(0))
            val packetXtoA = ibcX.chan(idChanXYZ).packets[seqXtoA]!!
            ibcA.recvPacketUnordered(
                    packetXtoA,
                    ibcX.chanProof(idChanXYZ),
                    ibcX.host().getCurrentHeight())
            val ackAtoX = ibcA.chan(idChanABC).acknowledgements[seqXtoA]!!
            ibcX.acknowledgePacketUnordered(
                    packetXtoA,
                    ackAtoX,
                    ibcA.chanProof(idChanABC),
                    ibcA.host().getCurrentHeight())
        }

        assertFailsWith<ExecutionException> {
            ibcX.sendTransfer(
                    idPortXYZ,
                    idChanXYZ,
                    Denom("${idPortXYZ.id}/${idChanXYZ.id}/JPY"),
                    Amount(1),
                    xKey,
                    aKey,
                    Height.getDefaultInstance(),
                    Timestamp(0))
        }
    }
    */
}