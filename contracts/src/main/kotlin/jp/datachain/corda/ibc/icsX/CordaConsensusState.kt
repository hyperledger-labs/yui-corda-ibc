package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.contracts.BelongsToContract
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class CordaConsensusState(val notaryKey: java.security.PublicKey, override val timestamp: Timestamp) : ConsensusState {
    override val participants: List<AbstractParty> = listOf()
}
