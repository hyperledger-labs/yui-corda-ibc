package jp.datachain.corda.ibc.flows.ics4

import co.paralleluniverse.fibers.Suspendable
import ibc.core.channel.v1.Tx
import jp.datachain.corda.ibc.flows.util.*
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.toFungibleTokenPacketData
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandlePacketRecv
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
class IbcRecvPacketFlow(
        private val baseId: StateRef,
        private val msg: Tx.MsgRecvPacket
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.single()
        val builder = TransactionBuilder(notary)

        val inputs = mutableListOf<StateAndRef<ContractState>>()
        val refs = mutableListOf<StateAndRef<ContractState>>()

        // query host from vault
        val host = serviceHub.vaultService.queryIbcHost(baseId)!!
        refs.add(host)

        // check participants
        val participants = host.state.data.participants.map{it as Party}
        require(participants.contains(ourIdentity))

        // states specific to ICS-20
        when (msg.packet.destinationPort) {
            "transfer" -> {
                val cashBank = serviceHub.vaultService.queryIbcCashBank(baseId)!!
                inputs.add(cashBank)

                val packet = msg.packet.data.toFungibleTokenPacketData()
                val denom = Denom.fromString(packet.denom)
                val source = denom.hasPrefix(Identifier(msg.packet.sourcePort), Identifier(msg.packet.sourceChannel))
                if (source && !denom.removePrefix().isVoucher()) {
                    val denom = denom.removePrefix()
                    val issuer = serviceHub.identityService.partyFromKey(denom.issuerKey)!!
                    val amount = AMOUNT(packet.amount, denom.currency).issuedBy(PartyAndReference(issuer, OpaqueBytes(ByteArray(1))))
                    val coins = serviceHub.vaultService.prepareCoins<Cash.State, Issued<Currency>>(
                            ownerKey = cashBank.state.data.owner.owningKey,
                            amount = amount)
                    require(coins.isNotEmpty())
                    inputs.addAll(coins)
                    builder.addCommand(Cash.Commands.Move(), cashBank.state.data.owner.owningKey)
                }
            }
            "transfer-old" -> {
                val bank = serviceHub.vaultService.queryIbcBank(baseId)!!
                inputs.add(bank)
            }
        }

        // query chan from vault
        val chanId = Identifier(msg.packet.destinationChannel)
        val chan = serviceHub.vaultService.queryIbcState<IbcChannel>(baseId, chanId)!!
        inputs.add(chan)

        // query conn from vault
        val connId = Identifier(chan.state.data.end.connectionHopsList.single())
        val conn = serviceHub.vaultService.queryIbcState<IbcConnection>(baseId, connId)!!
        refs.add(conn)

        // query client from vault
        val clientId = Identifier(conn.state.data.end.clientId)
        val client = serviceHub.vaultService.queryIbcState<IbcClientState>(baseId, clientId)!!
        refs.add(client)

        // create command and outputs
        val command = HandlePacketRecv(msg)
        val ctx = Context(inputs.map{it.state.data}, refs.map{it.state.data})
        val signers = listOf(ourIdentity.owningKey)
        command.execute(ctx, signers)

        // build tx
        builder.addCommand(command, signers)
        refs.forEach{builder.addReferenceState(ReferencedStateAndRef(it))}
        inputs.forEach{builder.addInputState(it)}
        ctx.outStates.forEach{builder.addOutputState(it)}

        val stx = serviceHub.signInitialTransaction(builder)

        val sessions = (participants - ourIdentity).map{initiateFlow(it)}
        return subFlow(FinalityFlow(stx, sessions))
    }
}

@InitiatedBy(IbcRecvPacketFlow::class)
class IbcRecvPacketResponderFlow(private val counterPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val stx = subFlow(ReceiveFinalityFlow(counterPartySession))
        println(stx)
    }
}