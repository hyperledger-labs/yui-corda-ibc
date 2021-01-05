package jp.datachain.corda.ibc.clients.corda

import jp.datachain.corda.ibc.conversion.into
import jp.datachain.corda.ibc.grpc.Corda
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics2.ConsensusState
import net.corda.core.contracts.StateRef
import java.security.PublicKey

data class CordaConsensusState(val consensusState: Corda.ConsensusState) : ConsensusState {
    constructor(baseId: StateRef, notaryKey: PublicKey): this(Corda.ConsensusState.newBuilder()
            .setBaseId(baseId.into())
            .setNotaryKey(notaryKey.into())
            .build())

    override fun clientType() = ClientType.CordaClient
    override fun getRoot() = throw NotImplementedError()
    override fun getTimestamp() = throw NotImplementedError()
    override fun validateBasic() = throw NotImplementedError()

    val baseId get() = consensusState.baseId.into()
    val notaryKey get() = consensusState.notaryKey.into()
}