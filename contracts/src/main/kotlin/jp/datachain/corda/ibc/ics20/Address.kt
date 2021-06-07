package jp.datachain.corda.ibc.ics20

import net.corda.core.crypto.Crypto
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.parseAsHex
import net.corda.core.utilities.toHexString
import org.bitcoinj.core.Bech32
import java.security.PublicKey

@CordaSerializable
class Address(bytes: ByteArray): OpaqueBytes(bytes) {
    companion object {
        private const val PREFIX = "cosmos"

        private fun decodeBech32(bech32: String) = Bech32.decode(bech32).let{
            require(it.hrp == PREFIX)
            it.data
        }

        private fun encodeBech32(value: ByteArray) = Bech32.encode(PREFIX, value)

        fun fromHex(hex: String) = Address(hex.parseAsHex())
        fun fromBech32(bech32: String) = Address(decodeBech32(bech32))
        fun fromPublicKey(pubkey: PublicKey) = Address(pubkey.encoded)
    }

    fun toHex() : String = bytes.toHexString()
    fun toBech32() : String = encodeBech32(bytes)
    fun toPublicKey() = Crypto.decodePublicKey(bytes)
}