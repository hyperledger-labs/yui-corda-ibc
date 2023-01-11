package jp.datachain.corda.ibc.flows.ics20cash

import co.paralleluniverse.fibers.Suspendable
import ibc.applications.transfer.v1.Transfer
import ibc.applications.transfer.v1.Tx
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.*
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.hasPrefixes
import jp.datachain.corda.ibc.ics20cash.HandleTransfer
import jp.datachain.corda.ibc.ics20cash.Voucher
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcClientState
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.AMOUNT
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.issuedBy
import java.util.*

@StartableByRPC
@InitiatingFlow
class IbcTransferFlow(
        private val baseId: StateRef,
        private val msg: Tx.MsgTransfer
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)

        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query chan from vault
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, Identifier(msg.sourceChannel))!!

        // query conn from vault
        val connId = Identifier(chan.state.data.end.connectionHopsList.single())
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<IbcClientState>(baseId, clientId)!!

        // query bank from vault
        val bank = serviceHub.vaultService.queryIbcCashBank(baseId)!!

        val inputs: MutableSet<StateAndRef<ContractState>> = mutableSetOf(chan, bank)
        val refs = setOf(host, client, conn)

        // prepare Cashes(or Vouchers) for inputs/outputs
        val denom: Denom = msg.token.denom.let {
            if (it.hasPrefixes("ibc")) {
                bank.state.data.resolveDenom(it)
            } else {
                Denom.fromString(it)
            }
        }
        val quantity = msg.token.amount.toLong()
        if (!denom.isVoucher()) {
            val amount = AMOUNT(quantity, denom.currency).issuedBy(PartyAndReference(bank.state.data.owner, OpaqueBytes(ByteArray(1))))
            val coins = serviceHub.vaultService.prepareCoins<Cash.State, Issued<Currency>>(
                    ownerKey = ourIdentity.owningKey,
                    amount = amount)
            require(coins.isNotEmpty())
            inputs.addAll(coins)
            builder.addCommand(Cash.Commands.Move(), ourIdentity.owningKey)
        } else {
            val amount = AMOUNT(quantity, denom.denomTrace)
            val coins = serviceHub.vaultService.prepareCoins<Voucher, Transfer.DenomTrace>(
                    ownerKey = ourIdentity.owningKey,
                    amount = amount)
            require(coins.isNotEmpty())
            inputs.addAll(coins)
        }

        // execute command for obtaining outputs
        val ctx = Context(
                inputs.map{it.state.data},
                refs.map{it.state.data}
        )
        val signers = listOf(ourIdentity.owningKey)
        val handler = HandleTransfer(msg)
        handler.execute(ctx, signers)

        // build transaction
        builder.addCommand(Ibc.DatagramHandlerCommand.HandleTransfer(handler), signers)
        refs.forEach { builder.addReferenceState(ReferencedStateAndRef(it)) }
        inputs.forEach { builder.addInputState(it) }
        ctx.outStates.forEach { builder.addOutputState(it) }

        // sign transaction (by initiator)
        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcTransferFlow::class)
class IbcTransferResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}