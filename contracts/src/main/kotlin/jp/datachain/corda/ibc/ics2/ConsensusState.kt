package jp.datachain.corda.ibc.ics2

import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface ConsensusState {
    val timestamp : Timestamp
    val height: Height
}