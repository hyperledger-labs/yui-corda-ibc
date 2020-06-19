package jp.datachain.cosmos.x.ibc.ics03_connection.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePrefix
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq

@ReqPath("ibc/connections/open-init")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConnectionOpenInitReq(
        val connectionID: String,
        val clientID: String,
        val counterpartyClientID: String,
        val counterpartyConnectionID: String,
        val counterpartyPrefix: MerklePrefix
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}