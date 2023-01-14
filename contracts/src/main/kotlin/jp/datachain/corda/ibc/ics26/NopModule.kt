package jp.datachain.corda.ibc.ics26

import com.google.protobuf.Any
import ibc.core.channel.v1.ChannelOuterClass.Packet
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

class NopModule : Module() {
    override val callbacks = NopModuleCallbacks()
    override fun createOutgoingPacket(ctx: Context, signers: Collection<PublicKey>, anyMsg: Any) {
        val packet = anyMsg.unpack<Packet>()
        Handler.sendPacket(ctx, packet)
    }
}