package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics2.Misbehaviour
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcClientState
import java.security.PublicKey

data class HandleClientMisbehaviour(
        val identifier: Identifier,
        val misbehaviour: Misbehaviour
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val client = ctx.getInput<IbcClientState>()
        require(client.id == identifier)
        val result = client.impl.checkMisbehaviourAndUpdateState(misbehaviour)
        ctx.addOutput(client.update(result))
    }
}