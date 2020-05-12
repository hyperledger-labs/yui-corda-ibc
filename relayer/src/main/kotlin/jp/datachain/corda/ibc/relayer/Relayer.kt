package jp.datachain.corda.ibc.relayer

import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import jp.datachain.corda.ibc.types.Version
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

        val clientAid = ibcA.host().generateIdentifier()
        val consensusStateB = ibcB.host().getConsensusState(Height(0)) as CordaConsensusState
        ibcA.createClient(clientAid, ClientType.CordaClient, consensusStateB)

        val clientBid = ibcB.host().generateIdentifier()
        val consensusStateA = ibcA.host().getConsensusState(Height(0)) as CordaConsensusState
        ibcB.createClient(clientBid, ClientType.CordaClient, consensusStateA)

        val connAid = ibcA.host().generateIdentifier()
        val connBid = ibcB.host().generateIdentifier()
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

        val portAid = ibcA.host().generateIdentifier()
        val chanAid = ibcA.host().generateIdentifier()
        val portBid = ibcB.host().generateIdentifier()
        val chanBid = ibcB.host().generateIdentifier()
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

        for (sequence in 1..10) {
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

        for (sequence in 1..10) {
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
