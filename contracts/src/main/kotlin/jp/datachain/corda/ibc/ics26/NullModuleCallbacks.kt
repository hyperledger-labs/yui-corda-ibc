package jp.datachain.corda.ibc.ics26

import ibc.core.connection.v1.Connection
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet

class NullModuleCallbacks: ModuleCallbacks {
    override fun onChanOpenInit(
            ctx: Context,
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Connection.Version){}

    override fun onChanOpenTry(
            ctx: Context,
            order: ChannelOrder,
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
            packet: Packet
    ) = Acknowledgement()

    override fun onTimeoutPacket(
            ctx: Context,
            packet: Packet){}

    override fun onAcknowledgePacket(
            ctx: Context,
            packet: Packet,
            acknowledgement: Acknowledgement){}

    override fun onTimeoutPacketClose(
            ctx: Context,
            packet: Packet){}
}