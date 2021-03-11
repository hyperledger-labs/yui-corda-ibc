package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import ibc.lightclients.fabric.v1.Fabric
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.types.Timestamp

data class FabricConsensusState(val fabricConsensusState: Fabric.ConsensusState) : ConsensusState {
    override val consensusState get() = Any.pack(fabricConsensusState, "")!!

    override fun clientType() = ClientType.FabricClient
    override fun getRoot() = throw NotImplementedError()
    override fun getTimestamp() = Timestamp(fabricConsensusState.timestamp)
    override fun validateBasic() = require(fabricConsensusState.timestamp != 0L)
}