package jp.datachain.corda.ibc.icsX

import jp.datachain.corda.ibc.contracts.Tao
import jp.datachain.corda.ibc.ics4.ChannelEnd
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.ChannelState
import jp.datachain.corda.ibc.types.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

@BelongsToContract(Tao::class)
data class CordaChannelEnd(
        val portIdentifier: Identifier,
        val channelIdentifier: Identifier,
        override val state: ChannelState,
        override val ordering: ChannelOrder,
        override val counterpartyPortIdentifier: Identifier,
        override val counterpartyChannelIdentifier: Identifier,
        override val connectionHops: Array<Identifier>,
        override val version: String
) : ChannelEnd, ContractState {
    override val participants: List<AbstractParty> = listOf()
}