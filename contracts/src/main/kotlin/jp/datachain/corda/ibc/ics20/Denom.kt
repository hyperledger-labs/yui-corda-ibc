package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.toHex

@CordaSerializable
data class Denom(val denom: String) {
    fun hasPrefix(portId: Identifier, chanId: Identifier): Boolean {
        denom.split('/').let {
            return it.size == 3 && it[0] == portId.id && it[1] == chanId.id
        }
    }

    fun addPrefix(portId: Identifier, chanId: Identifier) = Denom("${portId.id}/${chanId.id}/$denom")

    fun removePrefix(): Denom = denom.split('/').let {
        assert(it.size == 3)
        Denom(it[2])
    }

    fun hasIbcPrefix(): Boolean = denom.split('/').let {
        assert(it.size == 2)
        it[0] == "ibc"
    }

    val ibcDenom: Denom
        get() {
            val hash = SecureHash.sha256(denom).bytes.toHex()
            return Denom("ibc/$hash")
        }
}