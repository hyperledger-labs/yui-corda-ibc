package jp.datachain.corda.ibc.ics4

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class ChannelState {
    INIT,
    TRYOPEN,
    OPEN,
    CLOSED,
}