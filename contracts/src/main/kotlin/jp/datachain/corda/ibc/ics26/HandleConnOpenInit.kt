package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.types.Version
import java.security.PublicKey

data class HandleConnOpenInit(
        val identifier: Identifier,
        val desiredCounterpartyConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val clientIdentifier: Identifier,
        val counterpartyClientIdentifier: Identifier,
        val version: Version.Single?
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.connOpenInit(
                ctx,
                identifier,
                desiredCounterpartyConnectionIdentifier,
                counterpartyPrefix,
                clientIdentifier,
                counterpartyClientIdentifier,
                version)
    }
}