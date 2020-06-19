package jp.datachain.cosmos.x.ibc.ics03_connection.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePrefix

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class Counterparty(
        // identifies the client on the counterparty chain associated with a given connection.
        val clientID: String?,
        // identifies the connection end on the counterparty chain associated with a given connection.
        val connectionID: String?,
        // commitment merkle prefix of the counterparty chain
        val prefix: MerklePrefix
)