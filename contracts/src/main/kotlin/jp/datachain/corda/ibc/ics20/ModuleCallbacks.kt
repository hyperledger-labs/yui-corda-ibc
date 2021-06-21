package jp.datachain.corda.ibc.ics20

import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.ModuleCallbacks

class ModuleCallbacks: ModuleCallbacks {
    override fun onRecvPacket(ctx: Context, packet: ChannelOuterClass.Packet): ChannelOuterClass.Acknowledgement {
        val data = packet.data.toFungibleTokenPacketData()
        val denom = Denom.fromString(data.denom)
        val amount = Amount.fromLong(data.amount)
        val receiver = Address.fromBech32(data.receiver)

        val ackBuilder = ChannelOuterClass.Acknowledgement.newBuilder()
        val source = denom.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        val bank = ctx.getInput<Bank>()
        if (source) {
            try {
                val unprefixedDenom = denom.removePrefix()
                ctx.addOutput(bank.unlock(receiver, unprefixedDenom, amount))
                ackBuilder.result = ByteString.copyFrom(ByteArray(1){1})
            } catch (e: IllegalArgumentException) {
                ctx.addOutput(bank.copy())
                ackBuilder.error = e.message!!
            }
        } else {
            try {
                val prefixedDenom = denom.addPath(Identifier(packet.destinationPort), Identifier(packet.destinationChannel))
                ctx.addOutput(bank
                        .recordDenom(prefixedDenom)
                        .mint(receiver, prefixedDenom, amount))
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
        val data = packet.data.toFungibleTokenPacketData()
        val denom = Denom.fromString(data.denom)
        val amount = Amount.fromLong(data.amount)
        val sender = Address.fromBech32(data.sender)

        val source = !denom.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        val bank = ctx.getInput<Bank>()
        if (source) {
            ctx.addOutput(bank.unlock(sender, denom, amount))
        } else {
            ctx.addOutput(bank.mint(sender, denom, amount))
        }
    }
}