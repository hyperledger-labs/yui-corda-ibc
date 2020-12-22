package jp.datachain.corda.ibc.ics26

import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.types.Version
import java.security.PublicKey

data class HandleChanOpenAck(
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val counterpartyVersion: Version,
        val counterpartyChannelIdentifier: Identifier,
        val proofTry: CommitmentProof,
        val proofHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.chanOpenAck(
                ctx,
                portIdentifier,
                channelIdentifier,
                counterpartyVersion,
                counterpartyChannelIdentifier,
                proofTry,
                proofHeight)
    }
}