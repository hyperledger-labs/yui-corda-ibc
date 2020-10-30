package jp.datachain.corda.ibc.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Version(val version: String)
