package jp.datachain.cosmos.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ABCIMessageLog(
    val msgIndex: Int,
    val log: String,
    val events: StringEvents
)

typealias ABCIMessageLogs = List<ABCIMessageLog>
