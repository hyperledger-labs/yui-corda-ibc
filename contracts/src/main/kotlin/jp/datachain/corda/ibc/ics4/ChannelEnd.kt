package jp.datachain.corda.ibc.ics4

import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.types.Version
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class ChannelEnd(
        val state: ChannelState,
        val ordering: ChannelOrder,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val connectionHops: List<Identifier>,
        val version: Version
)