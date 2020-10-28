package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleClientCreate(
        val identifier: Identifier,
        val type: ClientType,
        val consensusState: ConsensusState
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.createClient(ctx, identifier, type, consensusState)
    }
}