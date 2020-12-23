package jp.datachain.corda.ibc.ics23

import com.google.protobuf.ByteString
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes

@CordaSerializable
class CommitmentProof(proof: ByteString): OpaqueBytes(proof.toByteArray()) {
    fun toByteString() = ByteString.copyFrom(bytes)!!
}