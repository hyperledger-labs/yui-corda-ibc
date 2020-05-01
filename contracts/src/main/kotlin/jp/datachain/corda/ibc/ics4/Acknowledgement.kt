package jp.datachain.corda.ibc.ics4

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Acknowledgement(val data: ByteArray)
