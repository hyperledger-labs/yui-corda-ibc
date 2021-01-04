package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import net.corda.core.contracts.StateRef
import java.security.PublicKey

data class CordaConsensusState(
        val baseId: StateRef,
        val notaryKey: PublicKey
) : ConsensusState {
    override fun clientType() = ClientType.CordaClient
    override fun getRoot() = throw NotImplementedError()
    override fun getTimestamp() = throw NotImplementedError()
    override fun validateBasic() = throw NotImplementedError()
}