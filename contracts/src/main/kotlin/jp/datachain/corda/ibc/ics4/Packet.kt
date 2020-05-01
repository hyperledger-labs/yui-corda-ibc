package jp.datachain.corda.ibc.ics4

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Packet(val data: ByteArray)
