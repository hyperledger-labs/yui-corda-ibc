package jp.datachain.corda.ibc.ics20cash

import ibc.applications.transfer.v1.Transfer
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.states.IbcFungibleState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable

@BelongsToContract(Ibc::class)
@CordaSerializable
data class Voucher(
        override val baseId: StateRef,
        override val amount: Amount<Transfer.DenomTrace>,
        override val owner: AbstractParty
): IbcFungibleState<Transfer.DenomTrace>(), OwnableState {
    override val participants get() = listOf(owner)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        throw NotImplementedError()
    }
}