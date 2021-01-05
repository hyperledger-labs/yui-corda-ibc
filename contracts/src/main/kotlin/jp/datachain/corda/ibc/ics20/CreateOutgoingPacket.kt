package jp.datachain.corda.ibc.ics20

import ibc.applications.transfer.v1.Transfer
import ibc.applications.transfer.v1.Tx
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.hexToByteArray
import java.security.PublicKey

data class CreateOutgoingPacket(val msg: Tx.MsgTransfer): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val denom = Denom(msg.token.denom)
        val amount = Amount(msg.token.amount)
        val sender = Crypto.decodePublicKey(msg.sender.hexToByteArray())
        val sourcePort = Identifier(msg.sourcePort)
        val sourceChannel = Identifier(msg.sourceChannel)

        require(signers.contains(sender))

        val bank: Bank = ctx.getInput<Bank>()
        val source = !denom.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            ctx.addOutput(bank.lock(sender, denom, amount))
        } else {
            ctx.addOutput(bank.burn(sender, denom.removePrefix(), amount))
        }

        val data = Transfer.FungibleTokenPacketData.newBuilder()
                .setDenom(msg.token.denom)
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
                .setData(data.toByteString())
                .setTimeoutHeight(msg.timeoutHeight)
                .setTimeoutTimestamp(msg.timeoutTimestamp)
                .build()
        Handler.sendPacket(ctx, packet)
    }
}