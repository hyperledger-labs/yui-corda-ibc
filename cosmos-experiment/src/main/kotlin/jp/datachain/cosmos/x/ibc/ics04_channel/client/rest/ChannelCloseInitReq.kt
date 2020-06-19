package jp.datachain.cosmos.x.ibc.ics04_channel.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq

@ReqPath("ibc/ports/%s/channels/%s/close-init")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
class ChannelCloseInitReq : CosmosRequest {
    override var baseReq: BaseReq? = null
}