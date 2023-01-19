package jp.datachain.corda.ibc.clients.fabric

import com.google.protobuf.Any
import jp.datachain.corda.ibc.ics2.ClientStateFactory

class FabricClientStateFactory : ClientStateFactory() {
    override fun createClientState(anyClientState: Any, anyConsensusState: Any) = FabricClientState(anyClientState, anyConsensusState)
}