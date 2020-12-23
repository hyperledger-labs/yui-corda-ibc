package jp.datachain.corda.ibc.ics26

import ibc.core.client.v1.Client.Height
import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import java.security.PublicKey

data class HandleChanOpenAck(
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val counterpartyVersion: Connection.Version,
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