package jp.datachain.corda.ibc.ics4

import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.types.Version

data class ChannelEnd(
        val state: ChannelState,
        val ordering: ChannelOrder,
        val counterpartyPortIdentifier: Identifier,
        val counterpartyChannelIdentifier: Identifier,
        val connectionHops: Array<Identifier>,
        val version: Version.Single
)