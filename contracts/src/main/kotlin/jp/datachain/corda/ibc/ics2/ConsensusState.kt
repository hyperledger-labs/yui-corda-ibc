package jp.datachain.corda.ibc.ics2

import com.google.protobuf.Any
import ibc.core.commitment.v1.Commitment
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface ConsensusState {
    val anyConsensusState: Any

    fun clientType(): ClientType // Consensus kind

    // GetRoot returns the commitment root of the consensus state,
    // which is used for key-value pair verification.
    fun getRoot(): Commitment.MerkleRoot

    // GetTimestamp returns the timestamp (in nanoseconds) of the consensus state
    fun getTimestamp(): Timestamp

    fun validateBasic()
}
