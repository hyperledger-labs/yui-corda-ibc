package jp.datachain.corda.ibc.ics4

import jp.datachain.corda.ibc.types.Identifier

interface ChannelEnd {
        val state: ChannelState
        val ordering: ChannelOrder
        val counterpartyPortIdentifier: Identifier
        val counterpartyChannelIdentifier: Identifier
        val connectionHops: Array<Identifier>
        val version: String
}