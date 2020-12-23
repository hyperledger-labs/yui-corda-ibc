package jp.datachain.corda.ibc.ics4

import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class ChannelEnd(
        val state: ChannelState,
        val ordering: ChannelOrder,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val connectionHops: List<Identifier>,
        val version: Connection.Version
)