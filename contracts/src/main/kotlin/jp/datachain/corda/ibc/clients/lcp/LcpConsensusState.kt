package jp.datachain.corda.ibc.clients.lcp

import com.google.protobuf.Any
import ibc.core.commitment.v1.Commitment
import ibc.lightclients.lcp.v1.Lcp
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.types.Timestamp

data class LcpConsensusState(override val anyConsensusState: Any) : ConsensusState {
    val consensusState = anyConsensusState.unpack<Lcp.ConsensusState>()

    override fun clientType(): ClientType = ClientType.LcpClient

    override fun getRoot(): Commitment.MerkleRoot {
        TODO("Not yet implemented")
    }

    override fun getTimestamp(): Timestamp {
        return Timestamp(consensusState.timestamp)
    }

    override fun validateBasic() {
        /* no-op */
        return
    }
}
