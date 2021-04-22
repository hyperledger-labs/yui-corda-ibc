package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import java.math.BigInteger

@BelongsToContract(Ibc::class)
data class Bank(
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        val allocated: MutableMap<Denom, MutableMap<Address, Amount>>,
        val locked: MutableMap<Denom, MutableMap<Address, Amount>>,
        val minted: MutableMap<Denom, MutableMap<Address, Amount>>,
        val denoms: MutableMap<Denom, Denom>
): IbcState {
    override val id = Identifier("bank")

    constructor(host: Host) : this(
            host.participants,
            host.baseId,
            mutableMapOf(),
            mutableMapOf(),
            mutableMapOf(),
            mutableMapOf())

    private fun deepCopy() = copy(
            allocated = allocated.deepCopy(),
            locked = locked.deepCopy(),
            minted = minted.deepCopy(),
            denoms = denoms.toMutableMap()
    )

    private fun MutableMap<Denom, MutableMap<Address, Amount>>.deepCopy() = toMutableMap().apply{
        entries.forEach{
            put(it.key, it.value.toMutableMap())
        }
    }

    private fun MutableMap<Denom, MutableMap<Address, Amount>>.up(denom: Denom, owner: Address, amount: Amount) {
        put(denom, getOrDefault(denom, mutableMapOf()).apply{
            put(owner, getOrDefault(owner, Amount(BigInteger.ZERO)) + amount)
        })
    }

    private fun MutableMap<Denom, MutableMap<Address, Amount>>.down(denom: Denom, owner: Address, amount: Amount) {
        val m = get(denom) ?: throw IllegalArgumentException("unknown denom: $denom")
        val balance = m[owner] ?: throw IllegalArgumentException("no fund: amount=$amount")
        require(balance >= amount){"insufficient fund: balance=$balance, amount=$amount"}
        m[owner] = balance - amount
    }

    fun allocate(owner: Address, denom: Denom, amount: Amount) = deepCopy().apply{
        allocated.up(denom, owner, amount)
    }

    fun lock(owner: Address, denom: Denom, amount: Amount) = deepCopy().apply{
        allocated.down(denom, owner, amount)
        locked.up(denom, owner, amount)
    }

    fun unlock(owner: Address, denom: Denom, amount: Amount) = deepCopy().apply{
        allocated.up(denom, owner, amount)
        locked.down(denom, owner, amount)
    }

    fun mint(owner: Address, denom: Denom, amount: Amount) = deepCopy().apply{
        minted.up(denom, owner, amount)
    }

    fun burn(owner: Address, denom: Denom, amount: Amount) = deepCopy().apply{
        minted.down(denom, owner, amount)
    }

    fun recordDenom(denom: Denom) = deepCopy().apply{
        denoms[denom.ibcDenom] = denom
    }

    fun resolveDenom(ibcDenom: Denom) = denoms[ibcDenom] ?: throw IllegalArgumentException()
}