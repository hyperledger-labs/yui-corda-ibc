package jp.datachain.corda.ibc.ics20

import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.ModuleCallbacks

class ModuleCallbacks: ModuleCallbacks {
    override fun onRecvPacket(ctx: Context, packet: ChannelOuterClass.Packet): ChannelOuterClass.Acknowledgement {
        val data = FungibleTokenPacketData.decode(packet.data.toByteArray())
        val ackBuilder = ChannelOuterClass.Acknowledgement.newBuilder()
        val source = data.denomination.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        val bank: Bank = ctx.getInput<Bank>()
        if (source) {
            try {
                ctx.addOutput(bank.unlock(data.receiver, data.denomination, data.amount))
                ackBuilder.result = ByteString.copyFrom(ByteArray(1){1})
            } catch (e: IllegalArgumentException) {
                ctx.addOutput(bank.copy())
                ackBuilder.error = e.message!!
            }
        } else {
            try {
                ctx.addOutput(bank.mint(data.receiver, data.denomination, data.amount))
                ackBuilder.result = ByteString.copyFrom(ByteArray(1){1})
            } catch (e: IllegalArgumentException) {
                ctx.addOutput(bank.copy())
                ackBuilder.error = e.message!!
            }
        }
        return ackBuilder.build()
    }

    override fun onAcknowledgePacket(ctx: Context, packet: ChannelOuterClass.Packet, acknowledgement: ChannelOuterClass.Acknowledgement) {
        when (acknowledgement.responseCase) {
            ChannelOuterClass.Acknowledgement.ResponseCase.RESULT ->
                ctx.addOutput(ctx.getInput<Bank>().copy())
            ChannelOuterClass.Acknowledgement.ResponseCase.ERROR ->
                refundTokens(ctx, packet)
            else -> throw java.lang.IllegalArgumentException()
        }
    }

    private fun refundTokens(ctx: Context, packet: ChannelOuterClass.Packet) {
        val data = FungibleTokenPacketData.decode(packet.data.toByteArray())
        val source = !data.denomination.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        val bank: Bank = ctx.getInput<Bank>()
        if (source) {
            ctx.addOutput(bank.unlock(data.sender, data.denomination, data.amount))
        } else {
            ctx.addOutput(bank.mint(data.sender, data.denomination, data.amount))
        }
    }
}