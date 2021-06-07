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
        val amount = Amount.fromString(msg.token.amount)
        val sender = Address.fromBech32(msg.sender)
        val sourcePort = Identifier(msg.sourcePort)
        val sourceChannel = Identifier(msg.sourceChannel)

        // resolve real denom
        val bank = ctx.getReference<Bank>()
        val denom =
                if (msg.token.denom.hasPrefixes("ibc"))
                    bank.resolveDenom(msg.token.denom)
                else
                    Denom.fromString(msg.token.denom)

        val source = !denom.hasPrefix(sourcePort, sourceChannel)
        if (source) {
            // verify cash owner
            val cashes = ctx.getInputs<Cash.State>()
            val cashOwner = cashes.map{it.owner}.distinct().single()
            require(cashOwner.owningKey == sender.toAnonParty().owningKey)

            // verify denom & amount
            val cashSum = cashes.map{it.amount}.sumOrThrow() // sumOrThrow ensures all Cashes have same token (= issuer + currency)
            require(cashSum.token == denom.toToken())
            require(cashSum.quantity == amount.toLong())

            // lock assets = transfer Cash from sender to Bank user
            ctx.addOutput(Cash.State(cashSum, bank.owner))
        } else {
            // verify voucher owner
            val vouchers = ctx.getInputs<Voucher>()
            val voucherOwner = vouchers.map{it.owner}.distinct().single()
            require(voucherOwner.owningKey == sender.toAnonParty().owningKey)

            // verify denom & amount
            val voucherSum = vouchers.map{it.amount}.sumOrThrow() // sumCash ensures all Vouchers have same token (= issuer + currency)
            require(voucherSum.token.issuer.party == bank.owner)
            require(voucherSum.token.product == denom.denomTrace)
            require(voucherSum.quantity == amount.toLong())

            // burn vouchers
            ctx.addOutput(ctx.getInput<Bank>().burn(denom, amount))
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
    }
}