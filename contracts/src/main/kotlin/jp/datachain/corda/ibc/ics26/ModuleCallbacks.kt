package jp.datachain.corda.ibc.ics26

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface ModuleCallbacks {
    fun onChanOpenInit(
            ctx: Context,
            order: ChannelOuterClass.Order,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Connection.Version
    ){
        throw NotImplementedError()
    }

    fun onChanOpenTry(
            ctx: Context,
            order: ChannelOuterClass.Order,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Connection.Version,
            counterpartyVersion: Connection.Version
    ) {
        throw NotImplementedError()
    }

    fun onChanOpenAck(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            version: Connection.Version
    ) {
        throw NotImplementedError()
    }

    fun onChanOpenConfirm(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        throw NotImplementedError()
    }

    fun onChanCloseInit(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        throw NotImplementedError()
    }

    fun onChanCloseConfirm(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier
    ) {
        throw NotImplementedError()
    }

    fun onRecvPacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet
    ): ChannelOuterClass.Acknowledgement {
        throw NotImplementedError()
    }

    fun onTimeoutPacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet
    ) {
        throw NotImplementedError()
    }

    fun onAcknowledgePacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet,
            acknowledgement: ChannelOuterClass.Acknowledgement
    ) {
        throw NotImplementedError()
    }

    fun onTimeoutPacketClose(
            ctx: Context,
            packet: ChannelOuterClass.Packet
    ) {
        throw NotImplementedError()
    }
}