package jp.datachain.corda.ibc.clients.lcp

import com.google.protobuf.ByteString
import ibc.core.client.v1.Client
import ibc.core.commitment.v1.Commitment
import org.web3j.rlp.RlpDecoder
import org.web3j.rlp.RlpString
import java.math.BigInteger

data class StateCommitment(
    val prefix: Commitment.MerklePrefix,
    val path: String,
    val value: ByteArray,
    val height: Client.Height,
    val stateId: ByteArray
) {

    companion object {
        fun rlpDecode(rlp: ByteString): StateCommitment? {
            val decoded = RlpDecoder.decode(rlp.toByteArray())
            if (decoded.values.size != 5) {
                return null
            }

            return StateCommitment(
                Commitment.MerklePrefix.newBuilder()
                    .setKeyPrefix(ByteString.copyFrom((decoded.values[0] as RlpString).bytes))
                    .build(),
                (decoded.values[1] as RlpString).asString(),
                (decoded.values[2] as RlpString).bytes,
                Client.Height.newBuilder()
                    .setRevisionNumber(BigInteger(1, (decoded.values[3] as RlpString).bytes.copyOfRange(0, 8)).toLong())
                    .setRevisionHeight(BigInteger(1, (decoded.values[3] as RlpString).bytes.copyOfRange(8, 16)).toLong())
                    .build(),
                (decoded.values[4] as RlpString).bytes
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateCommitment

        if (prefix != other.prefix) return false
        if (path != other.path) return false
        if (!value.contentEquals(other.value)) return false
        if (height != other.height) return false
        if (!stateId.contentEquals(other.stateId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prefix.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + value.contentHashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + stateId.contentHashCode()
        return result
    }
}
