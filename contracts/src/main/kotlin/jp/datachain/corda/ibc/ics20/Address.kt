package jp.datachain.corda.ibc.ics20

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Address(val address: String)