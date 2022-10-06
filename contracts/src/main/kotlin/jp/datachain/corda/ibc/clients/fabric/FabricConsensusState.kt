package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import ibc.lightclients.fabric.v1.Fabric
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.types.Timestamp

data class FabricConsensusState(override val anyConsensusState: Any) : ConsensusState {
    val consensusState = anyConsensusState.unpack<Fabric.ConsensusState>()

    override fun clientType() = ClientType.FabricClient
    override fun getRoot() = throw NotImplementedError()
    override fun getTimestamp() = Timestamp(consensusState.timestamp)
    override fun validateBasic() = require(consensusState.timestamp != 0L)
}