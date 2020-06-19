package jp.datachain.cosmos.types.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ResponseWithHeight(
        val height: Int,
        val result: JsonNode
) {
    inline fun <reified T>resultAs() = ObjectMapper().registerKotlinModule().convertValue<T>(result)
}
