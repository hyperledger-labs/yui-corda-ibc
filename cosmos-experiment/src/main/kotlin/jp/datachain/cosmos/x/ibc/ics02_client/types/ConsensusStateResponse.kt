package jp.datachain.cosmos.x.ibc.ics02_client.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.amino.DisfixWrapper
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePath
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConsensusStateResponse(
        val consensusState: DisfixWrapper, /*exported.ConsensusState*/
        val proof: MerkleProof?,
        val proofPath: MerklePath?,
        val proofHeight: String? /*uint64*/
)