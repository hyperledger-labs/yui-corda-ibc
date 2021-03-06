package jp.datachain.corda.ibc.ics26

import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.ics20.toAcknowledgement
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandlePacketAcknowledgement(val msg: Tx.MsgAcknowledgement): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val module = ModuleCallbacks.lookupModule(Identifier(msg.packet.destinationPort))
        val ack = msg.acknowledgement.toAcknowledgement()
        module.onAcknowledgePacket(ctx, msg.packet, ack)
        Handler.acknowledgePacket(
                ctx,
                msg.packet,
                ack,
                CommitmentProof(msg.proofAcked),
                msg.proofHeight)
    }
}