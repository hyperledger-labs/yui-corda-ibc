package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.types.Version
import java.security.PublicKey

data class HandleChanOpenInit(
        val order: ChannelOrder,
        val connectionHops: List<Identifier>,
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val version: Version
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        Handler.chanOpenInit(
                ctx,
                order,
                connectionHops,
                portIdentifier,
                channelIdentifier,
                counterpartyPortIdentifier,
                counterpartyChannelIdentifier,
                version)
    }
}