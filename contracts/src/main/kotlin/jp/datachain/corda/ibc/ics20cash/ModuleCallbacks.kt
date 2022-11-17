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
        val quantity = data.amount
        val receiver = Address.fromBech32(data.receiver)

        val bank = ctx.getInput<CashBank>()

        val source = denom.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        if (source) {
            // verify cash owner
            val cashes = ctx.getInputs<Cash.State>()
            val cashOwner = cashes.map{it.owner}.distinct().single()
            require(cashOwner == bank.owner)

            // verify denom & amount
            val cashSum = cashes.map{it.amount}.sumOrThrow() // sumOrThrow ensures all Cashes have same token (= issuer + currency)
            val denom = denom.removePrefix()
            val amount = net.corda.core.contracts.Amount.fromDecimal(quantity.toBigDecimal(), cashSum.token)
            require(!denom.isVoucher())
            require(cashSum.token.issuer.party.owningKey == denom.issuerKey)
            require(cashSum.token.product == denom.currency)
            require(cashSum >= amount)

            // unlock assets = transfer Cash from Bank user to receiver
            ctx.addOutput(bank)
            ctx.addOutput(Cash.State(amount, AnonymousParty(receiver.toPublicKey())))
            if (cashSum > amount) {
                ctx.addOutput(Cash.State(cashSum - amount, bank.owner))
            }
        } else {
            require(!denom.isVoucher())

            // prefix locak port/channel identifiers to denom
            val denom = denom.addPath(Identifier(packet.destinationPort), Identifier(packet.destinationChannel))

            // mint voucher
            val (bank, voucher) = bank.recordDenom(denom).mint(receiver, denom, Amount.fromString(quantity))
            ctx.addOutput(bank)
            ctx.addOutput(voucher)
        }

        return ChannelOuterClass.Acknowledgement.newBuilder()
                .setResult(ByteString.copyFrom(ByteArray(1){1}))
                .build()
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
        val quantity = data.amount
        val sender = Address.fromBech32(data.sender)

        val bank = ctx.getInput<CashBank>()

        val source = !denom.hasPrefix(Identifier(packet.sourcePort), Identifier(packet.sourceChannel))
        if (source) {
            require(!denom.isVoucher())

            // verify cash owner
            val cashes = ctx.getInputs<Cash.State>()
            val cashOwner = cashes.map{it.owner}.distinct().single()
            require(cashOwner == bank.owner)

            // verify denom & amount
            val cashSum = cashes.map{it.amount}.sumOrThrow() // sumOrThrow ensures all Cashes have same token (= issuer + currency)
            val amount = net.corda.core.contracts.Amount.fromDecimal(quantity.toBigDecimal(), cashSum.token)
            require(cashSum.token.issuer.party.owningKey == denom.issuerKey)
            require(cashSum.token.product == denom.currency)
            require(cashSum >= amount)

            // unlock assets = refund Cash from Bank user to sender
            ctx.addOutput(Cash.State(amount, AnonymousParty(sender.toPublicKey())))
            if (cashSum > amount) {
                ctx.addOutput(Cash.State(cashSum - amount, bank.owner))
            }
        } else {
            require(denom.isVoucher())

            // re-mint voucher
            val (bank, voucher) = bank.mint(sender, denom, Amount.fromString(quantity))
            ctx.addOutput(bank)
            ctx.addOutput(voucher)
        }
    }
}