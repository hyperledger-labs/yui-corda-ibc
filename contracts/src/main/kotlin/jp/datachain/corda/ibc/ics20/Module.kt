package jp.datachain.corda.ibc.ics20

import com.google.protobuf.Any
import ibc.applications.transfer.v1.Tx
import ibc.applications.transfer.v2.Packet
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.conversion.unpack
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.Module
import jp.datachain.corda.ibc.states.IbcChannel
import java.security.PublicKey

class Module : Module() {
    override val callbacks = ModuleCallbacks()
    override fun createOutgoingPacket(ctx: Context, signers: Collection<PublicKey>, anyMsg: Any) {
        val msg = anyMsg.unpack<Tx.MsgTransfer>()

        val amount = Amount.fromString(msg.token.amount)
        val sender = Address.fromBech32(msg.sender)
        val sourcePort = Identifier(msg.sourcePort)
        val sourceChannel = Identifier(msg.sourceChannel)

        val bank = ctx.getInput<Bank>()
        val denom =
                if (msg.token.denom.hasPrefixes("ibc"))
                    bank.resolveDenom(msg.token.denom)
                else
                    Denom.fromString(msg.token.denom)

        val source = !denom.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            ctx.addOutput(bank.lock(sender, denom, amount))
        } else {
            ctx.addOutput(bank.burn(sender, denom, amount))
        }

        val data = Packet.FungibleTokenPacketData.newBuilder()
                .setDenom(denom.toString())
                .setAmount(msg.token.amount)
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