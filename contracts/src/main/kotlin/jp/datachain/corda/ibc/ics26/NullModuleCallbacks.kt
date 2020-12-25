package jp.datachain.corda.ibc.ics26

import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.ics24.Identifier

class NullModuleCallbacks: ModuleCallbacks {
    override fun onChanOpenInit(
            ctx: Context,
            order: ChannelOuterClass.Order,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Connection.Version){}

    override fun onChanOpenTry(
            ctx: Context,
            order: ChannelOuterClass.Order,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Connection.Version,
            counterpartyVersion: Connection.Version) {}

    override fun onChanOpenAck(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            version: Connection.Version){}

    override fun onChanOpenConfirm(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier){}

    override fun onChanCloseInit(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier){}

    override fun onChanCloseConfirm(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier) {}

    override fun onRecvPacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet
    ) = ChannelOuterClass.Acknowledgement.getDefaultInstance()

    override fun onTimeoutPacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet){}

    override fun onAcknowledgePacket(
            ctx: Context,
            packet: ChannelOuterClass.Packet,
            acknowledgement: ChannelOuterClass.Acknowledgement){}

    override fun onTimeoutPacketClose(
            ctx: Context,
            packet: ChannelOuterClass.Packet){}
}