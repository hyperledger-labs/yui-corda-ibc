package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.ics2.Height
import java.security.PublicKey

data class HandlePacketRecv(
        val packet: Packet,
        val proof: CommitmentProof,
        val proofHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val module = ModuleCallbacks.lookupModule(packet.destPort)
        val acknowledgement = module.onRecvPacket(ctx, packet)
        Handler.recvPacket(
                ctx,
                packet,
                proof,
                proofHeight,
                acknowledgement)
    }
}