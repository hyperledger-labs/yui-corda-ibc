package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.Any
import ibc.lightclients.corda.v1.Corda
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState

data class CordaConsensusState(override val anyConsensusState: Any) : ConsensusState {
    val consensusState = anyConsensusState.unpack<Corda.ConsensusState>()

    override fun clientType() = ClientType.CordaClient
    override fun getRoot() = throw NotImplementedError()
    override fun getTimestamp() = throw NotImplementedError()
    override fun validateBasic() = throw NotImplementedError()
}