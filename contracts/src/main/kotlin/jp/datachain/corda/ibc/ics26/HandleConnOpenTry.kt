package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import java.security.PublicKey

data class HandleConnOpenTry(
        val desiredIdentifier: Identifier,
        val counterpartyChosenConnectionIdentifer: Identifier,
        val counterpartyConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val counterpartyClientIdentifier: Identifier,
        val clientIdentifier: Identifier,
        val counterpartyVersions: List<Version>,
        val proofInit: CommitmentProof,
        val proofConsensus: CommitmentProof,
        val proofHeight: Height,
        val consensusHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.connOpenTry(
                ctx,
                desiredIdentifier,
                counterpartyChosenConnectionIdentifer,
                counterpartyConnectionIdentifier,
                counterpartyPrefix,
                counterpartyClientIdentifier,
                clientIdentifier,
                counterpartyVersions,
                proofInit,
                proofConsensus,
                proofHeight,
                consensusHeight)
    }
}