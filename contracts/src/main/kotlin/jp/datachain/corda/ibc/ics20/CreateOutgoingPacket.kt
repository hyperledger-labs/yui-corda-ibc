package jp.datachain.corda.ibc.ics20

import ibc.applications.transfer.v1.Transfer
import ibc.applications.transfer.v1.Tx
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import java.security.PublicKey

data class CreateOutgoingPacket(val msg: Tx.MsgTransfer): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val denom = Denom(msg.token.denom)
        val amount = Amount(msg.token.amount)
        val sender = Address(msg.sender)
        val sourcePort = Identifier(msg.sourcePort)
        val sourceChannel = Identifier(msg.sourceChannel)

        val bank = ctx.getInput<Bank>()
        val resolvedDenom =
                if (denom.hasIbcPrefix())
                    bank.resolveDenom(denom)
                else
                    denom
        val source = !resolvedDenom.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            ctx.addOutput(bank.lock(sender, denom, amount))
        } else {
            ctx.addOutput(bank.burn(sender, denom, amount))
        }

        val data = Transfer.FungibleTokenPacketData.newBuilder()
                .setDenom(resolvedDenom.denom)
                .setAmount(msg.token.amount.toLong())
                .setSender(msg.sender)
                .setReceiver(msg.receiver)
                .build()

        val channel = ctx.getInput<IbcChannel>()
        val packet = ChannelOuterClass.Packet.newBuilder()
                .setSequence(channel.nextSequenceSend)
                .setSourcePort(msg.sourcePort)
                .setSourceChannel(msg.sourceChannel)
                .setDestinationPort(channel.end.counterparty.portId)
                .setDestinationChannel(channel.end.counterparty.channelId)
                .setData(data.toJson())
                .setTimeoutHeight(msg.timeoutHeight)
                .setTimeoutTimestamp(msg.timeoutTimestamp)
                .build()
        Handler.sendPacket(ctx, packet)
    }
}