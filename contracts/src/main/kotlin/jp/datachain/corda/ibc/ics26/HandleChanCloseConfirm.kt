package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics2.Height
import java.security.PublicKey

data class HandleChanCloseConfirm(
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val proofInit: CommitmentProof,
        val proofHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.chanCloseConfirm(
                ctx,
                portIdentifier,
                channelIdentifier,
                proofInit,
                proofHeight)
    }
}