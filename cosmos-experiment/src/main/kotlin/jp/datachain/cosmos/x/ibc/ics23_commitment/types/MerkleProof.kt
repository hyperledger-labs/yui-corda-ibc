package jp.datachain.cosmos.x.ibc.ics23_commitment.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.tendermint.crypto.merkle.Proof

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class MerkleProof(val proof: Proof?)