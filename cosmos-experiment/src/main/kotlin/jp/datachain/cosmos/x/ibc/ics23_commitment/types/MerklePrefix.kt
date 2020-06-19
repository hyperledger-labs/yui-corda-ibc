package jp.datachain.cosmos.x.ibc.ics23_commitment.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class MerklePrefix(val keyPrefix: ByteArray)