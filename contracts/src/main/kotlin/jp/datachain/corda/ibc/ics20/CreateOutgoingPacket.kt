package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.sendPacket
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Quadruple
import jp.datachain.corda.ibc.types.Timestamp
import net.corda.core.utilities.OpaqueBytes
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
    override fun verifySigners(signers: Collection<PublicKey>) = require(signers.contains(sender))

    override fun execute(ctx: Context) {
        val bank: Bank = ctx.getInput<Bank>()
        val source = !denomination.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            ctx.addOutput(bank.lock(sender, denomination, amount))
        } else {
            ctx.addOutput(bank.burn(sender, denomination, amount))
        }
        val data = FungibleTokenPacketData(
                denomination = denomination,
                amount = amount,
                sender = sender,
                receiver = receiver
        )
        val packet = Packet(
                data = OpaqueBytes(data.encode()),
                sourcePort = sourcePort,
                sourceChannel = sourceChannel,
                destPort = destPort,
                destChannel = destChannel,
                timeoutHeight = timeoutHeight,
                timeoutTimestamp = timeoutTimestamp,
                sequence = sequence
        )
        val chan: Channel = Quadruple(
                ctx.getReference<Host>(),
                ctx.getReference<ClientState>(),
                ctx.getReference<Connection>(),
                ctx.getInput<Channel>()
        ).sendPacket(packet)
        ctx.addOutput(chan)
    }
}