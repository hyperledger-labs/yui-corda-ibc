package jp.datachain.corda.ibc.ics3

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class ConnectionState {
    INIT,
    TRYOPEN,
    OPEN,
}