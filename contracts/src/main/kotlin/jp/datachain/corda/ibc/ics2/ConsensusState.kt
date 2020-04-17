package jp.datachain.corda.ibc.ics2

import jp.datachain.corda.ibc.types.Timestamp

interface ConsensusState {
    val timestamp : Timestamp
}