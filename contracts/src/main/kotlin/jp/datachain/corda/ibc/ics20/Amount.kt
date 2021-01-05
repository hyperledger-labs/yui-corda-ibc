package jp.datachain.corda.ibc.ics20

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

@CordaSerializable
data class Amount(val amount: BigInteger) {
    constructor(amount: Long): this(BigInteger.valueOf(amount))
    constructor(amount: String): this(amount.toBigInteger())

    operator fun plus(amount: Amount) = Amount(this.amount + amount.amount)
    operator fun minus(amount: Amount) = Amount(this.amount - amount.amount)
    operator fun compareTo(amount: Amount) = this.amount.compareTo(amount.amount)
}