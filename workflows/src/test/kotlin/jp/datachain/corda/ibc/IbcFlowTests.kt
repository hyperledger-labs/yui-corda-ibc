package jp.datachain.corda.ibc

import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.AutoAcceptable
import net.corda.core.node.NetworkParameters
import net.corda.core.utilities.OpaqueBytes
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.*

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
            it.registerInitiatedFlow(IbcHostSeedCreateResponderFlow::class.java)
            it.registerInitiatedFlow(IbcHostCreateResponderFlow::class.java)

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

    private fun toIdentifier(externalId: String, id: String) = Identifier(UniqueIdentifier(externalId, UUID.nameUUIDFromBytes(id.toByteArray(Charsets.US_ASCII))))

    @Test
    fun `relayer logic`() {
        val ibcA = CordaIbcClient(network, a)
        val ibcB = CordaIbcClient(network, x)

        ibcA.createHost(listOf(
                a.info.legalIdentities.single(),
                b.info.legalIdentities.single(),
                c.info.legalIdentities.single()
        ))
        ibcB.createHost(listOf(
                x.info.legalIdentities.single(),
                y.info.legalIdentities.single(),
                z.info.legalIdentities.single()
        ))

        val externalIdA = ibcA.host().linearId.externalId!!
        val externalIdB = ibcB.host().linearId.externalId!!

        val clientAid = toIdentifier(externalIdA, "client")
        val consensusStateB = ibcB.host().getConsensusState(Height(0))
        ibcA.createClient(clientAid, ClientType.CordaClient, consensusStateB)

        val clientBid = toIdentifier(externalIdB, "client")
        val consensusStateA = ibcA.host().getConsensusState(Height(0))
        ibcB.createClient(clientBid, ClientType.CordaClient, consensusStateA)

        val connAid = toIdentifier(externalIdA, "connection")
        val connBid = toIdentifier(externalIdB, "connection")
        ibcA.connOpenInit(
                connAid,
                connBid,
                ibcB.host().getCommitmentPrefix(),
                ibcA.client().id,
                ibcB.client().id)

        ibcB.connOpenTry(
                connBid,
                connAid,
                ibcA.host().getCommitmentPrefix(),
                ibcA.client().id,
                ibcB.client().id,
                ibcA.host().getCompatibleVersions(),
                ibcA.connProof(),
                ibcA.clientProof(),
                ibcA.host().getCurrentHeight(),
                ibcB.host().getCurrentHeight())

        ibcA.connOpenAck(
                connAid,
                ibcB.conn().end.version as Version.Single,
                ibcB.connProof(),
                ibcB.clientProof(),
                ibcB.host().getCurrentHeight(),
                ibcA.host().getCurrentHeight())

        ibcB.connOpenConfirm(
                connBid,
                ibcA.connProof(),
                ibcA.host().getCurrentHeight())

        val portAid = toIdentifier(externalIdA, "port")
        val chanAid = toIdentifier(externalIdA, "channel")
        val portBid = toIdentifier(externalIdB, "port")
        val chanBid = toIdentifier(externalIdB, "channel")
        ibcA.chanOpenInit(
                ChannelOrder.ORDERED,
                listOf(ibcA.conn().id),
                portAid,
                chanAid,
                portBid,
                chanBid,
                ibcA.conn().end.version as Version.Single)

        ibcB.chanOpenTry(
                ChannelOrder.ORDERED,
                listOf(ibcB.conn().id),
                portBid,
                chanBid,
                portAid,
                chanAid,
                ibcB.conn().end.version as Version.Single,
                ibcA.conn().end.version as Version.Single,
                ibcA.chanProof(),
                ibcA.host().getCurrentHeight())

        ibcA.chanOpenAck(
                portAid,
                chanAid,
                ibcB.chan().end.version,
                ibcB.chanProof(),
                ibcB.host().getCurrentHeight())

        ibcB.chanOpenConfirm(
                portBid,
                chanBid,
                ibcA.chanProof(),
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
                    ibcA.chanProof(),
                    ibcA.host().getCurrentHeight(),
                    ack)

            ibcA.acknowledgePacket(
                    packet,
                    ack,
                    ibcB.chanProof(),
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
                    ibcB.chanProof(),
                    ibcB.host().getCurrentHeight(),
                    ack)

            ibcB.acknowledgePacket(
                    packet,
                    ack,
                    ibcA.chanProof(),
                    ibcA.host().getCurrentHeight())
        }

        ibcA.chanCloseInit(
                portAid,
                chanAid)

        ibcB.chanCloseConfirm(
                portBid,
                chanBid,
                ibcA.chanProof(),
                ibcA.host().getCurrentHeight())
    }
}