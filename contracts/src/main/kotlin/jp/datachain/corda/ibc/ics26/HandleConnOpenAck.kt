package jp.datachain.corda.ibc.ics26

import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.types.Version
import java.security.PublicKey

data class HandleConnOpenAck(
        val identifier: Identifier,
        val version: Version,
        val counterpartyIdentifier: Identifier,
        val proofTry: CommitmentProof,
        val proofConsensus: CommitmentProof,
        val proofHeight: Height,
        val consensusHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.connOpenAck(
                ctx,
                identifier,
                version,
                counterpartyIdentifier,
                proofTry,
                proofConsensus,
                proofHeight,
                consensusHeight)
    }
}