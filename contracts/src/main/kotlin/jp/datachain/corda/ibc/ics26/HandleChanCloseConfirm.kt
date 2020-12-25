package jp.datachain.corda.ibc.ics26

import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleChanCloseConfirm(val msg: Tx.MsgChannelCloseConfirm): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.chanCloseConfirm(ctx, msg)
    }
}