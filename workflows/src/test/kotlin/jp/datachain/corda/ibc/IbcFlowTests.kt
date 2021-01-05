package jp.datachain.corda.ibc

import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
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
            it.registerInitiatedFlow(IbcGenesisCreateResponderFlow::class.java)
            it.registerInitiatedFlow(IbcHostAndBankCreateResponderFlow::class.java)
            it.registerInitiatedFlow(IbcFundAllocateResponderFlow::class.java)

            it.registerInitiatedFlow(IbcClientCreateResponderFlow::class.java)

            it.registerInitiatedFlow(IbcConnOpenInitResponderFlow::class.java)
            it.registerInitiatedFlow(IbcConnOpenTryResponderFlow::class.java)
            it.registerInitiatedFlow(IbcConnOpenAckResponderFlow::class.java)
            it.registerInitiatedFlow(IbcConnOpenConfirmResponderFlow::class.java)

            it.registerInitiatedFlow(IbcChanOpenInitResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanOpenTryResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanOpenAckResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanOpenConfirmResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanCloseInitResponderFlow::class.java)
            it.registerInitiatedFlow(IbcChanCloseConfirmResponderFlow::class.java)

            it.registerInitiatedFlow(IbcSendPacketResponderFlow::class.java)
            it.registerInitiatedFlow(IbcRecvPacketResponderFlow::class.java)
            it.registerInitiatedFlow(IbcAcknowledgePacketResponderFlow::class.java)

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

        ibcA.createHostAndBank(listOf(
                a.info.legalIdentities.single(),
                b.info.legalIdentities.single(),
                c.info.legalIdentities.single()
        ))
        ibcB.createHostAndBank(listOf(
                x.info.legalIdentities.single(),
                y.info.legalIdentities.single(),
                z.info.legalIdentities.single()
        ))

        val clientAid = Identifier("clientA")
        val consensusStateB = ibcB.host().getConsensusState(Height.getDefaultInstance())
        ibcA.createClient(clientAid, ClientType.CordaClient, consensusStateB)

        val clientBid = Identifier("clientB")
        val consensusStateA = ibcA.host().getConsensusState(Height.getDefaultInstance())
        ibcB.createClient(clientBid, ClientType.CordaClient, consensusStateA)

        val connAid = Identifier("connectionA")
        val connBid = Identifier("connectionB")
        ibcA.connOpenInit(
                connAid,
                connBid,
                ibcB.host().getCommitmentPrefix(),
                clientAid,
                clientBid,
                null)

        ibcB.connOpenTry(
                connBid,
                connBid,
                connAid,
                ibcA.host().getCommitmentPrefix(),
                clientAid,
                clientBid,
                ibcA.host().getCompatibleVersions(),
                ibcA.connProof(connAid),
                ibcA.clientProof(clientAid),
                ibcA.host().getCurrentHeight(),
                ibcB.host().getCurrentHeight())

        ibcA.connOpenAck(
                connAid,
                ibcB.conn(connBid).end.versionsList.single(),
                connBid,
                ibcB.connProof(connBid),
                ibcB.clientProof(clientBid),
                ibcB.host().getCurrentHeight(),
                ibcA.host().getCurrentHeight())

        ibcB.connOpenConfirm(
                connBid,
                ibcA.connProof(connAid),
                ibcA.host().getCurrentHeight())

        val portAid = Identifier("portA")
        val chanAid = Identifier("channelA")
        val portBid = Identifier("portB")
        val chanBid = Identifier("channelB")
        val order = ChannelOuterClass.Order.ORDER_UNORDERED
        ibcA.chanOpenInit(
                order,
                listOf(connAid),
                portAid,
                chanAid,
                portBid,
                chanBid,
                ibcA.conn(connAid).end.versionsList.single().toString())

        ibcB.chanOpenTry(
                order,
                listOf(connBid),
                portBid,
                chanBid,
                chanBid,
                portAid,
                chanAid,
                ibcB.conn(connBid).end.versionsList.single().toString(),
                ibcA.conn(connAid).end.versionsList.single().toString(),
                ibcA.chanProof(chanAid),
                ibcA.host().getCurrentHeight())

        ibcA.chanOpenAck(
                portAid,
                chanAid,
                ibcB.chan(chanBid).end.version,
                chanBid,
                ibcB.chanProof(chanBid),
                ibcB.host().getCurrentHeight())

        ibcB.chanOpenConfirm(
                portBid,
                chanBid,
                ibcA.chanProof(chanAid),
                ibcA.host().getCurrentHeight())

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

            ibcB.recvPacketUnordered(
                    packet,
                    ibcA.chanProof(chanAid),
                    ibcA.host().getCurrentHeight())

            ibcA.acknowledgePacketUnordered(
                    packet,
                    ChannelOuterClass.Acknowledgement.getDefaultInstance(),
                    ibcB.chanProof(chanBid),
                    ibcB.host().getCurrentHeight())
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

            ibcA.recvPacketUnordered(
                    packet,
                    ibcB.chanProof(chanBid),
                    ibcB.host().getCurrentHeight())

            ibcB.acknowledgePacketUnordered(
                    packet,
                    ChannelOuterClass.Acknowledgement.getDefaultInstance(),
                    ibcA.chanProof(chanAid),
                    ibcA.host().getCurrentHeight())
        }

        ibcA.chanCloseInit(
                portAid,
                chanAid)

        ibcB.chanCloseConfirm(
                portBid,
                chanBid,
                ibcA.chanProof(chanAid),
                ibcA.host().getCurrentHeight())
    }

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
                ibcY.host().getConsensusState(Height.getDefaultInstance()))
        // XYZ creates client for interacting with ABC
        ibcX.createClient(
                idCliXYZ,
                ClientType.CordaClient,
                ibcB.host().getConsensusState(Height.getDefaultInstance()))

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
}