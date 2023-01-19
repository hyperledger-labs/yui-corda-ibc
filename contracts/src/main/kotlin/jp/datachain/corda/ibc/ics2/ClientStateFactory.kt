package jp.datachain.corda.ibc.ics2

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
abstract class ClientStateFactory {
    abstract fun createClientState(anyClientState: com.google.protobuf.Any, anyConsensusState: com.google.protobuf.Any) : ClientState
    override fun equals(other: Any?) = other?.javaClass == javaClass
    override fun hashCode() = javaClass.name.hashCode()
}