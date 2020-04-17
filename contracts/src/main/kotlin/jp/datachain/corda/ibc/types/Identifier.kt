package jp.datachain.corda.ibc.types

import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes

@CordaSerializable
data class Identifier(val bytes: OpaqueBytes)
