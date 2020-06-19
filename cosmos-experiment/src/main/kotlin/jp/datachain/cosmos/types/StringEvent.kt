package jp.datachain.cosmos.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class StringEvent(
        val type: String?,
        val attributes: List<Attribute>?
)

typealias StringEvents = List<StringEvent>
