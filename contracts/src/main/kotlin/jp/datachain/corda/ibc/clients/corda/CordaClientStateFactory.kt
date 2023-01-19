package jp.datachain.corda.ibc.clients.corda

import com.google.protobuf.Any
import jp.datachain.corda.ibc.ics2.ClientStateFactory

class CordaClientStateFactory : ClientStateFactory() {
    override fun createClientState(anyClientState: Any, anyConsensusState: Any) = CordaClientState(anyClientState, anyConsensusState)
}