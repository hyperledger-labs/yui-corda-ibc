package jp.datachain.corda.ibc.ics20

import net.corda.core.crypto.Crypto
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.parseAsHex
import net.corda.core.utilities.toHexString
import org.bitcoinj.core.AddressFormatException
import org.bitcoinj.core.Bech32
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.security.PublicKey

@CordaSerializable
class Address(bytes: ByteArray): OpaqueBytes(bytes) {
    companion object {
        private const val PREFIX = "cosmos"

        private fun ByteArray.convertBits(fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
            assert(fromBits <= 8)
            assert(toBits <= 8)

            var acc = 0
            var bits = 0
            val out = ByteArrayOutputStream()
            val maxv = (1 shl toBits) - 1
            val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
            for (i in 0 until this.size) {
                val value: Int = this[i].toInt() and 0xff
                if (value ushr fromBits != 0) {
                    throw IllegalArgumentException("Input value '$value' exceeds '$fromBits' bit size")
                }
                acc = ((acc shl fromBits) or value) and maxAcc
                bits += fromBits
                while (bits >= toBits) {
                    bits -= toBits
                    out.write((acc ushr bits) and maxv)
                }
            }
            if (pad) {
                if (bits > 0) out.write((acc shl (toBits - bits)) and maxv)
            } else if (bits >= fromBits || ((acc shl (toBits - bits)) and maxv) != 0) {
                throw IllegalArgumentException("Could not convert bits, invalid padding")
            }
            return out.toByteArray()
        }

        private fun decodeBech32(bech32: String) = Bech32.decode(bech32).let{
            require(it.hrp == PREFIX)
            it.data.convertBits(5, 8, false)
        }

        private fun encodeBech32(value: ByteArray) = Bech32.encode(PREFIX, value.convertBits(8, 5, true))

        fun fromHex(hex: String) = Address(hex.parseAsHex())
        fun fromBech32(bech32: String) = Address(decodeBech32(bech32))
        fun fromPublicKey(pubkey: PublicKey) = Address(pubkey.encoded)
    }

    fun toHex() : String = bytes.toHexString()
    fun toBech32() : String = encodeBech32(bytes)
    fun toPublicKey() = Crypto.decodePublicKey(bytes)
}