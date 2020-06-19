package jp.datachain.cosmos.x.ibc.ics03_connection.client.rest

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jp.datachain.corda.ibc.cosmos.CosmosRequest
import jp.datachain.corda.ibc.cosmos.ReqPath
import jp.datachain.cosmos.types.rest.BaseReq
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerklePrefix
import jp.datachain.cosmos.x.ibc.ics23_commitment.types.MerkleProof

@ReqPath("ibc/connections/open-try")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class ConnectionOpenTryReq(
        val connectionID: String,
        val clientID: String,
        val counterpartyClientID: String,
        val counterpartyConnectionID: String,
        val counterpartyPrefix: MerklePrefix,
        val counterpartyVersions: List<String>,
        val proofInit: MerkleProof,
        val proofConsensus: MerkleProof,
        val proofHeight: String, /*uint64*/
        val consensusHeight: String /*uint64*/
) : CosmosRequest {
    override var baseReq: BaseReq? = null
}