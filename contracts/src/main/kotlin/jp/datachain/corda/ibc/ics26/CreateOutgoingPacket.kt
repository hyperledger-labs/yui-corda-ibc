package jp.datachain.corda.ibc.ics26

import com.google.protobuf.Any
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.states.IbcChannel
import java.security.PublicKey

class CreateOutgoingPacket(val anyMsg: Any) : DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val host = ctx.getReference<Host>()
        val chan = ctx.getInput<IbcChannel>()
        val module = host.lookupModule(chan.portId)
        module.createOutgoingPacket(ctx, signers, anyMsg)
    }
}