package jp.datachain.corda.ibc.ics20cash

import com.google.protobuf.ByteString
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.toFungibleTokenPacketData
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.ModuleCallbacks
import net.corda.core.contracts.Amount.Companion.sumOrThrow
import net.corda.core.identity.AnonymousParty
import net.corda.finance.contracts.asset.Cash

class ModuleCallbacks: ModuleCallbacks {
    override fun onRecvPacket(ctx: Context, packet: ChannelOuterClass.Packet): ChannelOuterClass.Acknowledgement {
        val data = packet.data.toFungibleTokenPacketData()
        val denom = Denom.fromString(data.denom)
        val amount = Amount.fromLong(data.amount)
        val receiver = Address.fromBech32(data.receiver)

        val ackBuilder = ChannelOuterClass.Acknowledgement.newBuilder()
        val source = denom.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        val bank = ctx.getInput<CashBank>()

        if (source) {
            try {
                // verify cash owner
                val cashes = ctx.getInputs<Cash.State>()
                val cashOwner = cashes.map{it.owner}.distinct().single()
                require(cashOwner == bank.owner)

                // verify denom & amount
                val cashSum = cashes.map{it.amount}.sumOrThrow() // sumOrThrow ensures all Cashes have same token (= issuer + currency)
                val unprefixedDenom = denom.removePrefix()
                require(cashSum.token == unprefixedDenom.toToken())
                require(cashSum.quantity == amount.toLong())

                // unlock assets = transfer Cash from Bank user to receiver
                ctx.addOutput(Cash.State(cashSum, AnonymousParty(receiver.toPublicKey())))

                ackBuilder.result = ByteString.copyFrom(ByteArray(1){1})
            } catch (e: IllegalArgumentException) {
                ctx.addOutput(bank.copy())
                ackBuilder.error = e.message!!
            }
        } else {
            try {
                // prefix locak port/channel identifiers to denom
                val denom = denom.addPath(Identifier(packet.destinationPort), Identifier(packet.destinationChannel))

                // mint voucher
                val (bank, voucher) = bank.recordDenom(denom).mint(receiver, denom, amount)
                ctx.addOutput(bank)
                ctx.addOutput(voucher)

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
                ctx.addOutput(ctx.getInput<CashBank>().copy())
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
        val bank = ctx.getInput<CashBank>()
        if (source) {
            // verify cash owner
            val cashes = ctx.getInputs<Cash.State>()
            val cashOwner = cashes.map{it.owner}.distinct().single()
            require(cashOwner == bank.owner)

            // verify denom & amount
            val cashSum = cashes.map{it.amount}.sumOrThrow() // sumOrThrow ensures all Cashes have same token (= issuer + currency)
            require(cashSum.token == denom.toToken())
            require(cashSum.quantity == amount.toLong())

            // unlock assets = refund Cash from Bank user to receiver
            ctx.addOutput(Cash.State(cashSum, AnonymousParty(sender.toPublicKey())))
        } else {
            // re-mint voucher
            val (bank, voucher) = bank.mint(sender, denom, amount)
            ctx.addOutput(bank)
            ctx.addOutput(voucher)
        }
    }
}