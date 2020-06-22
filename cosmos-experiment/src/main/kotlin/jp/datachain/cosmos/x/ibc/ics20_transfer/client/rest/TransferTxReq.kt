package jp.datachain.cosmos.x.ibc.ics20_transfer.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.Coins
import jp.datachain.cosmos.types.rest.BaseReq

@ReqPath("ibc/ports/%s/channels/%s/transfer")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class TransferTxReq(
        val destHeight: String, /*uint64*/
        val amount: Coins,
        val receiver: String
) : CosmosRequest {
        override var baseReq: BaseReq? = null
}