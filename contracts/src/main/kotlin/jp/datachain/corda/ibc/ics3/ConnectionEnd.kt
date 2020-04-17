package jp.datachain.corda.ibc.ics3

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.types.Identifier
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class ConnectionEnd(
        val connectionIdentifier: Identifier,
        val state: ConnectionState,
        val counterpartyConnectionIdentifier: Identifier,
        val counterpartyPrefix: CommitmentPrefix,
        val clientIdentifier: Identifier,
        val counterpartyClientIdentifier: Identifier,
        val version: Version
) : ContractState {
    override val participants: List<AbstractParty> = listOf()
}