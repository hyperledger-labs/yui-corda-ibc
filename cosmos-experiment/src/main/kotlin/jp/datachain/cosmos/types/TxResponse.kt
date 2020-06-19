package jp.datachain.cosmos.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.amino.DisfixWrapper

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class TxResponse(
        val height: String,
        val txhash: String,
        val codespace: String?,
        val code: Int?,
        val data: String?,
        val rawLog: String?,
        val logs: ABCIMessageLogs?,
        val info: String?,
        val gasWanted: String?,
        val gasUsed: String?,
        val tx: DisfixWrapper?,
        val timestamp: String?
)