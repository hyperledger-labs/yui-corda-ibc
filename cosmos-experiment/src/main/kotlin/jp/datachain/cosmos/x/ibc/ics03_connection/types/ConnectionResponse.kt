package jp.datachain.cosmos.x.ibc.ics03_connection.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePath
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConnectionResponse(
        val connection: ConnectionEnd,
        val proof: MerkleProof?,
        val proofPath: MerklePath?,
        val proofHeight: String?/*uint64*/
)