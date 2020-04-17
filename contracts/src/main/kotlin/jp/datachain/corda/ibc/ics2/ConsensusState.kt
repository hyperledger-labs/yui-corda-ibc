package jp.datachain.corda.ibc.ics2

import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.contracts.ContractState

interface ConsensusState : ContractState {
    val timestamp : Timestamp
}