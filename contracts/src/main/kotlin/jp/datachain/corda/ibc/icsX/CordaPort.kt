package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics5.Port
import jp.datachain.corda.ibc.types.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class CordaPort(
        val portIdentifier: Identifier,
        val channelIdentifiers: Collection<Identifier>
) : Port, ContractState {
    override val participants: List<AbstractParty> = listOf()
}