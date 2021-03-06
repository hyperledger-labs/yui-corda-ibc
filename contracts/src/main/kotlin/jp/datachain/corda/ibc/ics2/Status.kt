package jp.datachain.corda.ibc.ics2

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class Status {
    Active,
    Frozen,
    Expired,
    Unknown;
}