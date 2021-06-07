package jp.datachain.corda.ibc.ics20cash

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Issued
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class Bank(
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        val owner: AbstractParty,
        val supply: Map<Denom, Amount>,
        val denoms: Map<String, Denom>
): IbcState {
    override val id = Identifier("bank")

    constructor(host: Host, owner: AbstractParty) : this(
            host.participants,
            host.baseId,
            owner,
            emptyMap(),
            emptyMap())

    private fun setSupply(denom: Denom, amount: Amount): Bank = copy(supply = supply + Pair(denom, amount))

    fun mint(owner: Address, denom: Denom, amount: Amount): Pair<Bank, Voucher> {
        val bank = setSupply(denom, supply.getOrDefault(denom, Amount.ZERO) + amount)

        val owner = owner.toAnonParty()
        val amount = net.corda.core.contracts.Amount(amount.toLong(), Issued(owner.ref(), denom.denomTrace))
        val voucher = Voucher(participants, owner, amount)

        return Pair(bank, voucher)
    }

    fun burn(denom: Denom, amount: Amount): Bank {
        return setSupply(denom, (supply[denom]!! - amount).also{require(it > Amount.ZERO)})
    }

    fun recordDenom(denom: Denom): Bank = copy(denoms = denoms + Pair(denom.toIbcDenom(), denom))

    fun resolveDenom(ibcDenom: String) = denoms[ibcDenom] ?: throw IllegalArgumentException("unknown IBC denom: $ibcDenom")
}