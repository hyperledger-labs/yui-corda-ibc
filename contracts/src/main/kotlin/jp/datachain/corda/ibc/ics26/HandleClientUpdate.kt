package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.Header
import jp.datachain.corda.ibc.ics24.Identifier
import java.security.PublicKey

data class HandleClientUpdate(
        val identifier: Identifier,
        val header: Header
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val client = ctx.getInput<ClientState>()
        require(client.id == identifier)
        val result = client.checkHeaderAndUpdateState(header)
        ctx.addOutput(result.first)
    }
}