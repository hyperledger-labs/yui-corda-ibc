package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleChanCloseInit(
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.chanCloseInit(
                ctx,
                portIdentifier,
                channelIdentifier)
    }
}