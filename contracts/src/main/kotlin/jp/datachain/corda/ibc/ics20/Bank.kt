package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty
import java.math.BigDecimal
import java.security.PublicKey

@BelongsToContract(Ibc::class)
data class Bank(
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        val allocated: LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>,
        val locked: LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>,
        val minted: LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>
): IbcState {
    override val id = Identifier("bank")

    private inline fun <reified K, reified V> LinkedHashMap<K, V>.cloneAndPut(k: K, v: V): LinkedHashMap<K, V> {
        val m = clone() as LinkedHashMap<K, V>
        m.put(k, v)
        return m
    }

    private fun up(mm: LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>, denom: Denom, owner: PublicKey, amount: Amount)
            : LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>> {
        val m: LinkedHashMap<PublicKey, Amount> = mm.getOrDefault(denom, LinkedHashMap())
        val balance = m.getOrDefault(owner, Amount(BigDecimal.ZERO))
        return mm.cloneAndPut(denom, m.cloneAndPut(owner, balance + amount))
    }

    private fun down(mm: LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>>, denom: Denom, owner: PublicKey, amount: Amount)
            : LinkedHashMap<Denom, LinkedHashMap<PublicKey, Amount>> {
        val m: LinkedHashMap<PublicKey, Amount> = mm.get(denom) ?: throw IllegalArgumentException("unknown denomination")
        val balance = m.get(owner) ?: throw IllegalArgumentException("insufficient funds")
        assert(balance >= amount)
        return mm.cloneAndPut(denom, m.cloneAndPut(owner, balance - amount))
    }

    fun allocate(owner: PublicKey, denom: Denom, amount: Amount) = copy(
            allocated = up(allocated, denom, owner, amount)
    )

    fun lock(owner: PublicKey, denom: Denom, amount: Amount) = copy(
            allocated = down(allocated, denom, owner, amount),
            locked = up(locked, denom, owner, amount)
    )

    fun unlock(owner: PublicKey, denom: Denom, amount: Amount) = copy(
            allocated = up(allocated, denom, owner, amount),
            locked = down(locked, denom, owner, amount)
    )

    fun mint(owner: PublicKey, denom: Denom, amount: Amount) = copy(
            minted = up(minted, denom, owner, amount)
    )

    fun burn(owner: PublicKey, denom: Denom, amount: Amount) = copy(
            minted = down(minted, denom, owner, amount)
    )
}