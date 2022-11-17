package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics2.Header
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcClientState
import java.security.PublicKey

data class HandleClientUpdate(
        val identifier: Identifier,
        val header: Header
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val client = ctx.getInput<IbcClientState>()
        require(client.id == identifier)
        val result = client.impl.checkHeaderAndUpdateState(header)
        ctx.addOutput(client.update(result.first))
    }
}