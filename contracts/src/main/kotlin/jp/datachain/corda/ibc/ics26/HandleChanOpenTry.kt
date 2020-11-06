package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import java.security.PublicKey

data class HandleChanOpenTry(
        val order: ChannelOrder,
        val connectionHops: List<Identifier>,
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val counterpartyChosenChannelIdentifer: Identifier,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val version: Version,
        val counterpartyVersion: Version,
        val proofInit: CommitmentProof,
        val proofHeight: Height
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.chanOpenTry(
                ctx,
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyChosenChannelIdentifer,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version,
                counterpartyVersion,
                proofInit,
                proofHeight)
    }
}