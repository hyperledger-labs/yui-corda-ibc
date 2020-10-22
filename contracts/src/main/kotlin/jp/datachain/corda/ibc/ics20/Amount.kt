package jp.datachain.corda.ibc.ics20

import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal

@CordaSerializable
data class Amount(val amount: BigDecimal) {
    operator fun plus(amount: Amount) = Amount(this.amount + amount.amount)
    operator fun minus(amount: Amount) = Amount(this.amount - amount.amount)
    operator fun compareTo(amount: Amount) = this.amount.compareTo(amount.amount)
}