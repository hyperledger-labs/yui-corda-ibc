package jp.datachain.corda.ibc.flows.util

import net.corda.core.contracts.*
import net.corda.core.node.services.VaultService
import net.corda.core.node.services.queryBy
import java.security.PublicKey

inline fun <reified S, reified T: Any>VaultService.prepareCoins(
        ownerKey: PublicKey,
        amount: Amount<T>
): List<StateAndRef<S>>
        where S: FungibleState<T>,
              S: OwnableState
{
    val allCoins = this.queryBy<S>().states
            .filter { it.state.data.owner.owningKey == ownerKey && it.state.data.amount.token == amount.token }
            .sortedBy { it.state.data.amount } // ascending order
    val inputs = mutableListOf<StateAndRef<S>>()
    var collected = amount.copy(quantity = 0)
    for (coin in allCoins) {
        inputs.add(coin)
        collected += coin.state.data.amount
        if (collected >= amount) {
            break
        }
    }
    if (collected < amount) {
        throw InsufficientBalanceException(amount - collected)
    }
    return inputs.toList()
}