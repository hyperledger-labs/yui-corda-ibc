package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics2.ConsensusState
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Identifier
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class CordaConsensusState(
        val clientIdentifier: Identifier,
        val consensusStateHeight: Height,
        val notaryKey: java.security.PublicKey,
        override val timestamp: Timestamp
) : ConsensusState, ContractState {
    override val participants: List<AbstractParty> = listOf()
}
