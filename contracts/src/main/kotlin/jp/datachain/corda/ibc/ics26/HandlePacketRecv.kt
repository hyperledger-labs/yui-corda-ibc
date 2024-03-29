package jp.datachain.corda.ibc.ics26

import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandlePacketRecv(val msg: Tx.MsgRecvPacket): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val host = ctx.getReference<Host>()
        val callbacks = host.lookupModule(Identifier(msg.packet.destinationPort)).callbacks
        val acknowledgement = callbacks.onRecvPacket(ctx, msg.packet)
        Handler.recvPacket(
                ctx,
                msg.packet,
                CommitmentProof(msg.proofCommitment),
                msg.proofHeight,
                acknowledgement)
    }
}