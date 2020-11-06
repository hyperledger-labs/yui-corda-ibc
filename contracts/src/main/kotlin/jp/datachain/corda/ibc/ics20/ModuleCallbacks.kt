package jp.datachain.corda.ibc.ics20

import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.ModuleCallbacks
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.Packet
import net.corda.core.utilities.OpaqueBytes

class ModuleCallbacks: ModuleCallbacks {
    override fun onRecvPacket(ctx: Context, packet: Packet): Acknowledgement {
        val data = FungibleTokenPacketData.decode(packet.data.bytes)
        var ack = FungibleTokenPacketAcknowledgement()
        val source = data.denomination.hasPrefix(packet.sourcePort, packet.sourceChannel)
        val bank: Bank = ctx.getInput<Bank>()
        if (source) {
            try {
                ctx.addOutput(bank.unlock(data.receiver, data.denomination, data.amount))
            } catch (e: IllegalArgumentException) {
                ctx.addOutput(bank.copy())
                ack = FungibleTokenPacketAcknowledgement(e.message!!)
            }
        } else {
            try {
                ctx.addOutput(bank.mint(data.receiver, data.denomination, data.amount))
            } catch (e: IllegalArgumentException) {
                ctx.addOutput(bank.copy())
                ack = FungibleTokenPacketAcknowledgement(e.message!!)
            }
        }
        return Acknowledgement(OpaqueBytes(ack.encode()))
    }

    override fun onAcknowledgePacket(ctx: Context, packet: Packet, acknowledgement: Acknowledgement) {
        val ack = FungibleTokenPacketAcknowledgement.decode(acknowledgement.data!!.bytes)
        if (!ack.success) {
            refundTokens(ctx, packet)
        }
    }

    private fun refundTokens(ctx: Context, packet: Packet) {
        val data = FungibleTokenPacketData.decode(packet.data.bytes)
        val source = !data.denomination.hasPrefix(packet.sourcePort, packet.sourceChannel)
        val bank: Bank = ctx.getInput<Bank>()
        if (source) {
            ctx.addOutput(bank.unlock(data.sender, data.denomination, data.amount))
        } else {
            ctx.addOutput(bank.mint(data.sender, data.denomination, data.amount))
        }
    }
}