package jp.datachain.corda.ibc.ics26

import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.ics20.toAcknowledgement
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandlePacketAcknowledgement(val msg: Tx.MsgAcknowledgement): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val host = ctx.getReference<Host>()
        val callbacks = host.lookupModule(Identifier(msg.packet.destinationPort)).callbacks
        val ack = msg.acknowledgement.toAcknowledgement()
        callbacks.onAcknowledgePacket(ctx, msg.packet, ack)
        Handler.acknowledgePacket(
                ctx,
                msg.packet,
                ack,
                CommitmentProof(msg.proofAcked),
                msg.proofHeight)
    }
}