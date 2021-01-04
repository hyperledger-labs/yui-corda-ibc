package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.Misbehaviour
import jp.datachain.corda.ibc.ics24.Identifier
import java.security.PublicKey

data class HandleClientMisbehaviour(
        val identifier: Identifier,
        val misbehaviour: Misbehaviour
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val client = ctx.getInput<ClientState>()
        require(client.id == identifier)
        ctx.addOutput(client.checkMisbehaviourAndUpdateState(misbehaviour))
    }
}