package jp.datachain.cosmos.x.ibc.ics04_channel.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePath
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class PacketResponse(
        val packet: Packet,
        val proof: MerkleProof?,
        val proofPath: MerklePath?,
        val proofHeight: String? /*uint64*/
)