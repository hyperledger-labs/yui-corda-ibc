package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics3.ConnectionEnd
import jp.datachain.corda.ibc.ics3.ConnectionState
import jp.datachain.corda.ibc.types.Identifier
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class CordaConnectionEnd(
        val connectionIdentifier: Identifier,
        override val state: ConnectionState,
        override val counterpartyConnectionIdentifier: Identifier,
        override val counterpartyPrefix: CommitmentPrefix,
        override val clientIdentifier: Identifier,
        override val counterpartyClientIdentifier: Identifier,
        override val version: Version
) : ConnectionEnd, ContractState {
    override val participants: List<AbstractParty> = listOf()
}
