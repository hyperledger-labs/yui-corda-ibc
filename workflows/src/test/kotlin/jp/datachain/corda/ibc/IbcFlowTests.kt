package jp.datachain.corda.ibc

import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import jp.datachain.corda.ibc.types.Version
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

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
        val consensusStateB = ibcB.host().getConsensusState(Height(0))
        ibcA.createClient(clientAid, ClientType.CordaClient, consensusStateB)

        val clientBid = Identifier("clientB")
        val consensusStateA = ibcA.host().getConsensusState(Height(0))
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
                ibcB.conn(connBid).end.version,
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
        ibcA.chanOpenInit(
                ChannelOrder.ORDERED,
                listOf(connAid),
                portAid,
                chanAid,
                portBid,
                chanBid,
                ibcA.conn(connAid).end.version)

        ibcB.chanOpenTry(
                ChannelOrder.ORDERED,
                listOf(connBid),
                portBid,
                chanBid,
                portAid,
                chanAid,
                ibcB.conn(connBid).end.version,
                ibcA.conn(connAid).end.version,
                ibcA.chanProof(chanAid),
                ibcA.host().getCurrentHeight())

        ibcA.chanOpenAck(
                portAid,
                chanAid,
                ibcB.chan(chanBid).end.version,
                ibcB.chanProof(chanBid),
                ibcB.host().getCurrentHeight())

        ibcB.chanOpenConfirm(
                portBid,
                chanBid,
                ibcA.chanProof(chanAid),
                ibcA.host().getCurrentHeight())

        for (sequence in 1L..10) {
            val packet = Packet(
                    OpaqueBytes("Hello, Bob! (${sequence})".toByteArray()),
                    portAid,
                    chanAid,
                    portBid,
                    chanBid,
                    Height(0),
                    Timestamp(0),
                    sequence)
            ibcA.sendPacket(packet)

            val ack = Acknowledgement(OpaqueBytes("Thank you, Alice! (${sequence})".toByteArray()))
            ibcB.recvPacket(
                    packet,
                    ibcA.chanProof(chanAid),
                    ibcA.host().getCurrentHeight(),
                    ack)

            ibcA.acknowledgePacket(
                    packet,
                    ack,
                    ibcB.chanProof(chanBid),
                    ibcB.host().getCurrentHeight())
        }

        for (sequence in 1L..10) {
            val packet = Packet(
                    OpaqueBytes("Hello, Alice! (${sequence})".toByteArray()),
                    portBid,
                    chanBid,
                    portAid,
                    chanAid,
                    Height(0),
                    Timestamp(0),
                    sequence)
            ibcB.sendPacket(packet)

            val ack = Acknowledgement(OpaqueBytes("Thank you, Bob! (${sequence})".toByteArray()))
            ibcA.recvPacket(
                    packet,
                    ibcB.chanProof(chanBid),
                    ibcB.host().getCurrentHeight(),
                    ack)

            ibcB.acknowledgePacket(
                    packet,
                    ack,
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
    fun `create outgoing packet`() {
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

        ibcC.allocateFund(aKey, Denom("JPY"), Amount(1000))
        ibcC.allocateFund(bKey, Denom("JPY"), Amount(2000))
        ibcC.allocateFund(cKey, Denom("JPY"), Amount(3000))
        ibcZ.allocateFund(xKey, Denom("USD"), Amount(10000))
        ibcZ.allocateFund(yKey, Denom("USD"), Amount(20000))
        ibcZ.allocateFund(zKey, Denom("USD"), Amount(30000))

        val idCliABC = Identifier("clientABC")
        val idCliXYZ = Identifier("clientXYZ")

        ibcA.createClient(
                idCliABC,
                ClientType.CordaClient,
                ibcY.host().getConsensusState(Height(0)))
        ibcX.createClient(
                idCliXYZ,
                ClientType.CordaClient,
                ibcB.host().getConsensusState(Height(0)))

        val idConnABC = Identifier("connABC")
        val idConnXYZ = Identifier("connXYZ")

        ibcB.connOpenInit(
                idConnABC,
                idConnXYZ,
                ibcZ.host().getCommitmentPrefix(),
                idCliABC,
                idCliXYZ,
                null)
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
        ibcC.connOpenAck(
                idConnABC,
                ibcX.conn(idConnXYZ).end.version,
                idConnXYZ,
                ibcX.connProof(idConnXYZ),
                ibcX.clientProof(idCliXYZ),
                ibcX.host().getCurrentHeight(),
                ibcX.host().getCurrentHeight())
        ibcZ.connOpenConfirm(
                idConnXYZ,
                ibcA.connProof(idConnABC),
                ibcA.host().getCurrentHeight())

        val idPortABC = Identifier("portABC")
        val idPortXYZ = Identifier("portXYZ")
        val idChanABC = Identifier("chanABC")
        val idChanXYZ = Identifier("chanXYZ")
        val order = ChannelOrder.ORDERED

        ibcA.chanOpenInit(
                order,
                listOf(idConnABC),
                idPortABC,
                idChanABC,
                idPortXYZ,
                idChanXYZ,
                ibcB.conn(idConnABC).end.version)
        ibcX.chanOpenTry(
                order,
                listOf(idConnXYZ),
                idPortXYZ,
                idChanXYZ,
                idPortABC,
                idChanABC,
                ibcY.conn(idConnXYZ).end.version,
                ibcB.chan(idChanABC).end.version,
                ibcB.chanProof(idChanABC),
                ibcB.host().getCurrentHeight())
        ibcB.chanOpenAck(
                idPortABC,
                idChanABC,
                ibcZ.chan(idChanXYZ).end.version,
                ibcZ.chanProof(idChanXYZ),
                ibcZ.host().getCurrentHeight())
        ibcY.chanOpenConfirm(
                idPortXYZ,
                idChanXYZ,
                ibcC.chanProof(idChanABC),
                ibcC.host().getCurrentHeight())

        val denom = Denom("JPY")
        val amount = Amount(100)
        val destPort = idPortXYZ
        val destChannel = idChanXYZ
        val sourcePort = idPortABC
        val sourceChannel = idChanABC
        val timeoutHeight = Height(0)
        val timeoutTimestamp = Timestamp(0)
        ibcC.transfer(
                denom,
                amount,
                cKey,
                zKey,
                destPort,
                destChannel,
                sourcePort,
                sourceChannel,
                timeoutHeight,
                timeoutTimestamp
        )
        ibcA.transfer(
                denom,
                amount,
                aKey,
                xKey,
                destPort,
                destChannel,
                sourcePort,
                sourceChannel,
                timeoutHeight,
                timeoutTimestamp
        )
        ibcB.transfer(
                denom,
                amount,
                bKey,
                yKey,
                destPort,
                destChannel,
                sourcePort,
                sourceChannel,
                timeoutHeight,
                timeoutTimestamp
        )
    }
}