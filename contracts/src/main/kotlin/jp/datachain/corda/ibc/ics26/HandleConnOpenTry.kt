package jp.datachain.corda.ibc.ics26

import ibc.core.connection.v1.Tx
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleConnOpenTry(val msg: Tx.MsgConnectionOpenTry): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.connOpenTry(ctx, msg)
    }
}