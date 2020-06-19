package jp.datachain.cosmos.x.ibc.ics03_connection.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof

@ReqPath("ibc/connections/%s/open-ack")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConnectionOpenAckReq(
        val proofTry: MerkleProof,
        val proofConsensus: MerkleProof,
        val proofHeight: String, /*uint64*/
        val consensusHeight: String, /*uint64*/
        val version: String
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}