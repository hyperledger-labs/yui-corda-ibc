package jp.datachain.corda.ibc.ics20

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

@CordaSerializable
data class Amount(private val amount: BigInteger) {
    companion object {
        val ZERO = Amount(BigInteger.ZERO)

        fun fromLong(amount: Long) = Amount(BigInteger.valueOf(amount))
        fun fromString(amount: String) = Amount(amount.toBigInteger())
    }

    fun toLong() = amount.longValueExact()
    override fun toString() = amount.toString()

    operator fun plus(amount: Amount) = Amount(this.amount + amount.amount)
    operator fun minus(amount: Amount) = Amount(this.amount - amount.amount)
    operator fun compareTo(amount: Amount) = this.amount.compareTo(amount.amount)
}