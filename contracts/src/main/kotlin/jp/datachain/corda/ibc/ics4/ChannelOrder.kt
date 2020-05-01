package jp.datachain.corda.ibc.ics4

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class ChannelOrder {
    ORDERED,
    UNORDERED,
}