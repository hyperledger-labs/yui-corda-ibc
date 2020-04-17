package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.types.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class CordaAcknowledgement(
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        val sequence: Int,
        override val data: ByteArray
) : Acknowledgement, ContractState {
    override val participants: List<AbstractParty> = listOf()
}