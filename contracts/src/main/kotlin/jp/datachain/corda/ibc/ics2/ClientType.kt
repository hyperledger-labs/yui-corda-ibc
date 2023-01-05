package jp.datachain.corda.ibc.ics2

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class ClientType(private val string: String) {
    CordaClient("corda-ibc"),
    FabricClient("hyperledgerfabric"),
    SoloMachineClient("06-solomachine"),
    TendermintClient("07-tendermint"),
    LocalhostClient("09-localhost"),
    LcpClient("lcp-client");

    override fun toString() = string

    companion object {
        fun fromString(s: String): ClientType {
            return values().single{it.string == s}
        }
    }
}
