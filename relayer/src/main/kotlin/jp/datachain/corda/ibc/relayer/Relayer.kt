package jp.datachain.corda.ibc.relayer

import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.utilities.OpaqueBytes

object Relayer {
    @JvmStatic
    fun main(args: Array<String>) {
        val ibcA = CordaIbcClient("localhost", 10006)
        val ibcB = CordaIbcClient("localhost", 10009)

        ibcA.start()
        ibcB.start()

        ibcA.createHost(listOf("PartyA"))
        ibcB.createHost(listOf("PartyB"))

        val clientAid = Identifier("client")
        val consensusStateB = ibcB.host().getConsensusState(Height(0))
        ibcA.createClient(clientAid, ClientType.CordaClient, consensusStateB)

        val clientBid = Identifier("client")
        val consensusStateA = ibcA.host().getConsensusState(Height(0))
        ibcB.createClient(clientBid, ClientType.CordaClient, consensusStateA)

        val connAid = Identifier("connection")
        val connBid = Identifier("connection")
        ibcA.connOpenInit(
                connAid,
                connBid,
                ibcB.host().getCommitmentPrefix(),
                ibcA.client().id,
                ibcB.client().id,
                null)

        ibcB.connOpenTry(
                connBid,
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
                ibcB.conn().end.version,
                connBid,
                ibcB.connProof(),
                ibcB.clientProof(),
                ibcB.host().getCurrentHeight(),
                ibcA.host().getCurrentHeight())

        ibcB.connOpenConfirm(
                connBid,
                ibcA.connProof(),
                ibcA.host().getCurrentHeight())

        val portAid = Identifier("port")
        val chanAid = Identifier("channel")
        val portBid = Identifier("port")
        val chanBid = Identifier("channel")
        ibcA.chanOpenInit(
                ChannelOrder.ORDERED,
                listOf(ibcA.conn().id),
                portAid,
                chanAid,
                portBid,
                chanBid,
                ibcA.conn().end.version)

        ibcB.chanOpenTry(
                ChannelOrder.ORDERED,
                listOf(ibcB.conn().id),
                portBid,
                chanBid,
                chanBid,
                portAid,
                chanAid,
                ibcB.conn().end.version,
                ibcA.conn().end.version,
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
