package jp.datachain.corda.ibc.ics20cash

import ibc.applications.transfer.v1.Transfer
import ibc.applications.transfer.v1.Tx
import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics20.*
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import net.corda.core.contracts.Amount.Companion.sumOrThrow
import net.corda.finance.contracts.asset.Cash
import java.security.PublicKey

data class HandleTransfer(val msg: Tx.MsgTransfer): DatagramHandler {
    override fun execute(ctx: Context, signers: Collection<PublicKey>) {
        val quantity = msg.token.amount.toLong()
        val sender = Address.fromBech32(msg.sender)
        val sourcePort = Identifier(msg.sourcePort)
        val sourceChannel = Identifier(msg.sourceChannel)

        // resolve real denom
        val bank = ctx.getInput<CashBank>()
        val denom =
                if (msg.token.denom.hasPrefixes("ibc"))
                    bank.resolveDenom(msg.token.denom)
                else
                    Denom.fromString(msg.token.denom)

        val source = !denom.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            require(!denom.isVoucher()) { "Vouchers cannot be transferred to yet another blockchain" }

            // verify cash owner
            val cashes = ctx.getInputs<Cash.State>()
            val cashOwner = cashes.map{it.owner}.distinct().single()
            require(cashOwner.owningKey == sender.toPublicKey()) { "A sender's cashes must be consumed"}

            // verify denom & amount
            val cashSum = cashes.map{it.amount}.sumOrThrow() // sumOrThrow ensures all Cashes have same token (= issuer + currency)
            val amount = net.corda.core.contracts.Amount.fromDecimal(quantity.toBigDecimal(), cashSum.token)
            require(cashSum.token.issuer.party.owningKey == denom.issuerKey) { "Issuers specified by Denom and Cash must be equal" }
            require(cashSum.token.product == denom.currency) { "Currencies specified by Denom and Cash must be equal" }
            require(cashSum >= amount) { "Cash amount must be greater than or equal to an amount to be transferred" }

            // lock assets = transfer Cash from sender to Bank user
            ctx.addOutput(bank)
            ctx.addOutput(Cash.State(amount, bank.owner))
            if (cashSum > amount) {
                ctx.addOutput(Cash.State(cashSum - amount, cashOwner))
            }
        } else {
            // verify voucher owner
            val vouchers = ctx.getInputs<Voucher>()
            val voucherOwner = vouchers.map{it.owner}.distinct().single()
            require(voucherOwner.owningKey == sender.toPublicKey()) { "A sender's vouchers must be consumed"}

            // verify denom & amount
            val voucherSum = vouchers.map{it.amount}.sumOrThrow() // sumCash ensures all Vouchers have same token (= issuer + currency)
            val amount = net.corda.core.contracts.Amount.fromDecimal(quantity.toBigDecimal(), voucherSum.token)
            require(voucherSum.token == denom.denomTrace) { "Denominations specified by Denom and Voucher must be equal" }
            require(voucherSum >= amount) { "Voucher amount must be greater than or equal to an amount to be transferred" }

            // burn vouchers
            ctx.addOutput(bank.burn(denom, Amount.fromLong(quantity)))
            if (voucherSum > amount) {
                ctx.addOutput(Voucher(bank.baseId, voucherSum - amount, voucherOwner))
            }
        }

        val data = Transfer.FungibleTokenPacketData.newBuilder()
                .setDenom(denom.toString())
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

        require(signers.contains(sender.toPublicKey())) { "A sender must sign on this tx" }
    }
}