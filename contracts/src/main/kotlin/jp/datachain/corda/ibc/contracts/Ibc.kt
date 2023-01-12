package jp.datachain.corda.ibc.contracts

import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20cash.CashBank
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.DatagramHandler
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

class Ibc : Contract {
    override fun verify(tx: LedgerTransaction) {
        val miscCommands = tx.commandsOfType<MiscCommands>()
        val datagramHandlerCommands = tx.commandsOfType<DatagramHandlerCommand>()

        when (Pair(miscCommands.size, datagramHandlerCommands.size)) {
            Pair(1, 0) -> {
                miscCommands.single().value.verify(tx)
            }
            Pair(0, 1) -> {
                val command = datagramHandlerCommands.single()
                val ctx = Context(tx.inputsOfType(), tx.referenceInputsOfType())
                command.value.handler.execute(ctx, command.signers)
                ctx.verifyResults(tx.outputsOfType())
            }
            else -> throw IllegalArgumentException("unacceptable number of commands")
        }
    }

    sealed class MiscCommands : CommandData {
        abstract fun verify(tx: LedgerTransaction)
        override fun equals(other: Any?) = other?.javaClass == javaClass
        override fun hashCode() = javaClass.name.hashCode()

        class GenesisCreate : MiscCommands() {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "No state should be consumed" using (tx.inputs.isEmpty())
                "Exactly one state should be created" using (tx.outputs.size == 1)
                "Output type should be HostIdentifier" using (tx.outputs.single().data is Genesis)
            }
        }

        data class HostCreate(val moduleNames: Map<Identifier, String>) : MiscCommands() {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val genesis = tx.inRefsOfType<Genesis>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val expectedHost = Host(genesis, moduleNames)
                "Output should be expected states" using (newHost == expectedHost)
            }
        }

        class BankCreate : MiscCommands() {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val host = tx.inRefsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newBank = tx.outputsOfType<Bank>().single()
                val expectedBank = Bank(host.state.data)
                val expectedHost = host.state.data.addBank(expectedBank.id)
                "Output should be expected states" using (Pair(newHost, newBank) == Pair(expectedHost, expectedBank))
            }
        }

        data class CashBankCreate(val owner: Party) : MiscCommands() {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val signers = tx.commands.requireSingleCommand<CashBankCreate>().signers
                val host = tx.inRefsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newBank = tx.outputsOfType<CashBank>().single()
                val expectedBank = CashBank(host.state.data, owner)
                val expectedHost = host.state.data.addBank(expectedBank.id)
                "Signed by bank owner" using (signers.contains(owner.owningKey))
                "Output should be expected states" using (Pair(newHost, newBank) == Pair(expectedHost, expectedBank))
            }
        }

        data class FundAllocate(val owner: Address, val denom: Denom, val amount: Amount): MiscCommands() {
            override fun verify(tx: LedgerTransaction) {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val bank = tx.inputsOfType<Bank>().single()
                val newBank = tx.outputsOfType<Bank>().single()
                val expectedBank = bank.allocate(owner, denom, amount)
                "Output should be expected state" using (newBank == expectedBank)
            }
        }
    }

    sealed class DatagramHandlerCommand : CommandData {
        abstract val handler : DatagramHandler

        data class HandleClientCreate(override val handler: jp.datachain.corda.ibc.ics26.HandleClientCreate) : DatagramHandlerCommand()
        data class HandleClientUpdate(override val handler: jp.datachain.corda.ibc.ics26.HandleClientUpdate) : DatagramHandlerCommand()
        data class HandleClientMisbehaviour(override val handler: jp.datachain.corda.ibc.ics26.HandleClientMisbehaviour) : DatagramHandlerCommand()
        data class HandleConnOpenInit(override val handler: jp.datachain.corda.ibc.ics26.HandleConnOpenInit) : DatagramHandlerCommand()
        data class HandleConnOpenTry(override val handler: jp.datachain.corda.ibc.ics26.HandleConnOpenTry) : DatagramHandlerCommand()
        data class HandleConnOpenAck(override val handler: jp.datachain.corda.ibc.ics26.HandleConnOpenAck) : DatagramHandlerCommand()
        data class HandleConnOpenConfirm(override val handler: jp.datachain.corda.ibc.ics26.HandleConnOpenConfirm) : DatagramHandlerCommand()
        data class HandleChanOpenInit(override val handler: jp.datachain.corda.ibc.ics26.HandleChanOpenInit) : DatagramHandlerCommand()
        data class HandleChanOpenTry(override val handler: jp.datachain.corda.ibc.ics26.HandleChanOpenTry) : DatagramHandlerCommand()
        data class HandleChanOpenAck(override val handler: jp.datachain.corda.ibc.ics26.HandleChanOpenAck) : DatagramHandlerCommand()
        data class HandleChanOpenConfirm(override val handler: jp.datachain.corda.ibc.ics26.HandleChanOpenConfirm) : DatagramHandlerCommand()
        data class HandleChanCloseInit(override val handler: jp.datachain.corda.ibc.ics26.HandleChanCloseInit) : DatagramHandlerCommand()
        data class HandleChanCloseConfirm(override val handler: jp.datachain.corda.ibc.ics26.HandleChanCloseConfirm) : DatagramHandlerCommand()
        data class HandlePacketRecv(override val handler: jp.datachain.corda.ibc.ics26.HandlePacketRecv) : DatagramHandlerCommand()
        data class HandlePacketAcknowledgement(override val handler: jp.datachain.corda.ibc.ics26.HandlePacketAcknowledgement) : DatagramHandlerCommand()
        data class CreateOutgoingPacket(override val handler: jp.datachain.corda.ibc.ics26.CreateOutgoingPacket) : DatagramHandlerCommand()
    }
}