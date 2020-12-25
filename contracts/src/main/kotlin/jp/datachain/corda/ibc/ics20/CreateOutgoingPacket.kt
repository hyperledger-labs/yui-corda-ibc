package jp.datachain.corda.ibc.ics20

import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.client.v1.Client.Height
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.types.Timestamp
import java.security.PublicKey

data class CreateOutgoingPacket(
        val denomination: Denom,
        val amount: Amount,
        val sender: PublicKey,
        val receiver: PublicKey,
        val destPort: Identifier,
        val destChannel: Identifier,
        val sourcePort: Identifier,
        val sourceChannel: Identifier,
        val timeoutHeight: Height,
        val timeoutTimestamp: Timestamp,
        val sequence: Long
): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        require(signers.contains(sender))

        val bank: Bank = ctx.getInput<Bank>()
        val source = !denomination.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            ctx.addOutput(bank.lock(sender, denomination, amount))
        } else {
            ctx.addOutput(bank.burn(sender, denomination.removePrefix(), amount))
        }

        val data = FungibleTokenPacketData(
                denomination = denomination,
                amount = amount,
                sender = sender,
                receiver = receiver
        )
        val packet = ChannelOuterClass.Packet.newBuilder()
                .setSequence(sequence)
                .setSourcePort(sourcePort.id)
                .setSourceChannel(sourceChannel.id)
                .setDestinationPort(destPort.id)
                .setDestinationChannel(destChannel.id)
                .setData(ByteString.copyFrom(data.encode()))
                .setTimeoutHeight(timeoutHeight)
                .setTimeoutTimestamp(timeoutTimestamp.timestamp)
                .build()
        Handler.sendPacket(ctx, packet)
    }
}