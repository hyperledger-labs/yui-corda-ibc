package jp.datachain.corda.ibc.ics4

import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.OpaqueBytes

@CordaSerializable
data class Packet(
        val data: OpaqueBytes,
        val sourcePort: Identifier,
        val sourceChannel: Identifier,
        val destPort: Identifier,
        val destChannel: Identifier,
        val timeoutHeight: Height,
        val timeoutTimestamp: Timestamp,
        val sequence: Long)
