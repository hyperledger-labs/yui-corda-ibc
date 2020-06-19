package jp.datachain.amino

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class DisfixWrapper(
        val type: String,
        val value: JsonNode
) {
    inline fun <reified T>valueAs() = ObjectMapper().registerKotlinModule().convertValue<T>(value)
}