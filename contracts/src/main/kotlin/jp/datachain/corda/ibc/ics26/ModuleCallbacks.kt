package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.types.Version

interface ModuleCallbacks {
    fun onChanOpenInit(
            ctx: Context,
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version.Single
    ){
        throw NotImplementedError()
    }

    fun onChanOpenTry(
            ctx: Context,
            order: ChannelOrder,
            connectionHops: List<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version.Single,
            counterpartyVersion: Version.Single
    ) {
        throw NotImplementedError()
    }

    fun onChanOpenAck(
            ctx: Context,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            version: Version.Single
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
            packet: Packet
    ): Acknowledgement {
        throw NotImplementedError()
    }

    fun onTimeoutPacket(
            ctx: Context,
            packet: Packet
    ) {
        throw NotImplementedError()
    }

    fun onAcknowledgePacket(
            ctx: Context,
            packet: Packet,
            acknowledgement: Acknowledgement
    ) {
        throw NotImplementedError()
    }

    fun onTimeoutPacketClose(
            ctx: Context,
            packet: Packet
    ) {
        throw NotImplementedError()
    }
}