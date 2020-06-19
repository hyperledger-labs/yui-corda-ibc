package jp.datachain.cosmos.x.ibc.ics03_connection.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof

@ReqPath("ibc/connections/%s/open-confirm")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConnectionOpenConfirmReq(
    val proofAck: MerkleProof,
    val proofHeight: String /*uint64*/
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}