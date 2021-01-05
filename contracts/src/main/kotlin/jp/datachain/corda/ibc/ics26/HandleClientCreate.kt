package jp.datachain.corda.ibc.ics26

import ibc.core.client.v1.Client
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleClientCreate(val msg: Client.MsgCreateClient): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.createClient(ctx, msg)
    }
}