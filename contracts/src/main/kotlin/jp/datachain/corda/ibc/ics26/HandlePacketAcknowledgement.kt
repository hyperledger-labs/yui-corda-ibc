package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Height
import java.security.PublicKey

data class HandlePacketAcknowledgement(
        val packet: Packet,
        val acknowledgement: Acknowledgement,
        val proof: CommitmentProof,
        val proofHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val module = ModuleCallbacks.lookupModule(packet.destPort)
        module.onAcknowledgePacket(ctx, packet, acknowledgement)
        Handler.acknowledgePacket(
                ctx,
                packet,
                acknowledgement,
                proof,
                proofHeight)
    }
}