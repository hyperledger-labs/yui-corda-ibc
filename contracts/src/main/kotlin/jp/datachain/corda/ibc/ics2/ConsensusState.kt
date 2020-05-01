package jp.datachain.corda.ibc.ics2

import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface ConsensusState {
    val timestamp : Timestamp
    val height: Height
}