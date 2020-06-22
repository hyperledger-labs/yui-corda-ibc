package jp.datachain.cosmos.x.ibc.ics04_channel.types

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
data class Packet(
        // number corresponds to the order of sends and receives, where a Packet with
        // an earlier sequence number must be sent and received before a Packet with a
        // later sequence number.
        val sequence: String?, /*uint64*/
        // identifies the port on the sending chain.
        val sourcePort: String?,
        // identifies the channel end on the sending chain.
        val sourceChannel: String?,
        // identifies the port on the receiving chain.
        val destinationPort: String?,
        // identifies the channel end on the receiving chain.
        val destinationChannel: String?,
        // actual opaque bytes transferred directly to the application module
        val data: ByteArray?,
        // block height after which the packet times out
        val timeoutHeight: String?, /*uint64*/
        // block timestamp (in nanoseconds) after which the packet times out
        val timeoutTimestamp: String? /*uint64*/
)