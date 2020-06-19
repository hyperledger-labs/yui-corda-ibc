package jp.datachain.cosmos.x.ibc.ics03_connection.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.cosmos.x.ibc.types.State

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConnectionEnd(
    // connection identifier.
    val id: String?,
    // client associated with this connection.
    val clientID: String?,
    // opaque string which can be utilised to determine encodings or protocols for
    // channels or packets utilising this connection
    val versions: List<String>?,
    // current state of the connection end.
    val state: State?,
    // counterparty chain associated with this connection.
    val counterparty: Counterparty
)
