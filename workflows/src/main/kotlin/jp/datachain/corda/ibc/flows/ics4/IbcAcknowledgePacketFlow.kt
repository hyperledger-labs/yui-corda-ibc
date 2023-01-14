package jp.datachain.corda.ibc.flows.ics4

import co.paralleluniverse.fibers.Suspendable
import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.flows.util.queryIbcBank
import jp.datachain.corda.ibc.flows.util.queryIbcCashBank
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.flows.util.queryIbcState
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandlePacketAcknowledgement
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcClientState
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class IbcAcknowledgePacketFlow(
        private val baseId: StateRef,
        private val msg: Tx.MsgAcknowledgement
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // query bank if necessary
        val cashBankOrNull =
                if (msg.packet.destinationPort == "transfer")
                    serviceHub.vaultService.queryIbcCashBank(baseId)!!
                else
                    null
        val bankOrNull =
                if (msg.packet.destinationPort == "transfer-old")
                    serviceHub.vaultService.queryIbcBank(baseId)!!
                else
                    null

        // query chan from vault
        val chanId = Identifier(msg.packet.sourceChannel)
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, chanId)!!

        // query conn from vault
        val connId = Identifier(chan.state.data.end.connectionHopsList.single())
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<IbcClientState>(baseId, clientId)!!

        // create command and outputs
        val handler = HandlePacketAcknowledgement(msg)
        val inputs : MutableSet<IbcState> = mutableSetOf(chan.state.data)
        cashBankOrNull?.let{inputs.add(it.state.data)}
        bankOrNull?.let{inputs.add(it.state.data)}
        val ctx = Context(inputs, setOf(host, client, conn).map{it.state.data})
        val signers = listOf(ourIdentity.owningKey)
        handler.execute(ctx, signers)

        // build tx
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)
                .addCommand(Ibc.DatagramHandlerCommand.HandlePacketAcknowledgement(handler), signers)
                .addReferenceState(ReferencedStateAndRef(host))
                .addReferenceState(ReferencedStateAndRef(client))
                .addReferenceState(ReferencedStateAndRef(conn))
                .addInputState(chan)
        cashBankOrNull?.let{builder.addInputState(it)}
        bankOrNull?.let{builder.addInputState(it)}
        ctx.outStates.forEach{builder.addOutputState(it)}

        val tx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(tx, sessions))
    }
}

@InitiatedBy(IbcAcknowledgePacketFlow::class)
class IbcAcknowledgePacketResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}