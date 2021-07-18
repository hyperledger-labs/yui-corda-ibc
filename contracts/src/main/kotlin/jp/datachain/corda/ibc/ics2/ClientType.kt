package jp.datachain.corda.ibc.ics2

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class ClientType(private val string: String) {
    CordaClient("corda"),
    FabricClient("fabric"),
    SoloMachineClient("06-solomachine"),
    TendermintClient("07-tendermint"),
    LocalhostClient("09-localhost");

    override fun toString() = string

    companion object {
        fun fromString(s: String): ClientType {
            return values().single{it.string == s}
        }
    }
}