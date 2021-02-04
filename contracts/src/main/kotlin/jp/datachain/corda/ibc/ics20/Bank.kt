package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import java.math.BigInteger

@BelongsToContract(Ibc::class)
data class Bank(
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        val allocated: LinkedHashMap<Denom, LinkedHashMap<Address, Amount>>,
        val locked: LinkedHashMap<Denom, LinkedHashMap<Address, Amount>>,
        val minted: LinkedHashMap<Denom, LinkedHashMap<Address, Amount>>
): IbcState {
    override val id = Identifier("bank")

    constructor(genesisAndRef: StateAndRef<Genesis>) : this(
            genesisAndRef.state.data.participants,
            genesisAndRef.ref,
            linkedMapOf(),
            linkedMapOf(),
            linkedMapOf())

    private inline fun <reified K, reified V> LinkedHashMap<K, V>.cloneAndPut(k: K, v: V): LinkedHashMap<K, V> {
        val m = clone() as LinkedHashMap<K, V>
        m.put(k, v)
        return m
    }

    private fun up(mm: LinkedHashMap<Denom, LinkedHashMap<Address, Amount>>, denom: Denom, owner: Address, amount: Amount)
            : LinkedHashMap<Denom, LinkedHashMap<Address, Amount>> {
        val m: LinkedHashMap<Address, Amount> = mm.getOrDefault(denom, LinkedHashMap())
        val balance = m.getOrDefault(owner, Amount(BigInteger.ZERO))
        return mm.cloneAndPut(denom, m.cloneAndPut(owner, balance + amount))
    }

    private fun down(mm: LinkedHashMap<Denom, LinkedHashMap<Address, Amount>>, denom: Denom, owner: Address, amount: Amount)
            : LinkedHashMap<Denom, LinkedHashMap<Address, Amount>> {
        val m: LinkedHashMap<Address, Amount> = mm.get(denom) ?: throw IllegalArgumentException("unknown denomination")
        val balance = m.get(owner) ?: throw IllegalArgumentException("insufficient funds")
        require(balance >= amount)
        return mm.cloneAndPut(denom, m.cloneAndPut(owner, balance - amount))
    }

    fun allocate(owner: Address, denom: Denom, amount: Amount) = copy(
            allocated = up(allocated, denom, owner, amount)
    )

    fun lock(owner: Address, denom: Denom, amount: Amount) = copy(
            allocated = down(allocated, denom, owner, amount),
            locked = up(locked, denom, owner, amount)
    )

    fun unlock(owner: Address, denom: Denom, amount: Amount) = copy(
            allocated = up(allocated, denom, owner, amount),
            locked = down(locked, denom, owner, amount)
    )

    fun mint(owner: Address, denom: Denom, amount: Amount) = copy(
            minted = up(minted, denom, owner, amount)
    )

    fun burn(owner: Address, denom: Denom, amount: Amount) = copy(
            minted = down(minted, denom, owner, amount)
    )
}