package jp.datachain.corda.ibc.ics26

import ibc.core.client.v1.Tx
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleClientCreate(val msg: Tx.MsgCreateClient): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.createClient(ctx, msg)
    }
}