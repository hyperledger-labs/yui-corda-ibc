package jp.datachain.corda.ibc.contracts

import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics25.Handler
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class Ibc : Contract {
    override fun verify(tx: LedgerTransaction) {
        val commandSigners = tx.commands.single()
        val command = commandSigners.value
        val signers = commandSigners.signers
        when (command) {
            is Commands -> {
                command.verify(tx)
            }
            is DatagramHandler -> {
                val ctx = Context(tx.inputsOfType<IbcState>(), tx.referenceInputsOfType<IbcState>())
                command.execute(ctx, signers)
                ctx.verifyResults(tx.outputsOfType<IbcState>())
            }
        }
    }

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction)

        class GenesisCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "No state should be consumed" using (tx.inputs.size == 0)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                "Output type should be HostIdentifier" using (tx.outputs.single().data is Genesis)
            }
        }

        class HostAndBankCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 2)
                val genesis = tx.inRefsOfType<Genesis>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newBank = tx.outputsOfType<Bank>().single()
                val expectedHost = Host(genesis)
                val expectedBank = Bank(genesis)
                "Output should be expected states" using (Pair(newHost, newBank) == Pair(expectedHost, expectedBank))
            }
        }

        data class FundAllocate(val owner: PublicKey, val denom: Denom, val amount: Amount): Commands {
            override fun verify(tx: LedgerTransaction) {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val bank = tx.inputsOfType<Bank>().single()
                val newBank = tx.outputsOfType<Bank>().single()
                val expectedBank = bank.allocate(owner, denom, amount)
                "Output should be expected state" using (newBank == expectedBank)
            }
        }

        data class SendPacket(val packet: ChannelOuterClass.Packet) : Commands {
            override fun verify(tx: LedgerTransaction) {
                val ctx = Context(tx.inputsOfType<IbcState>(), tx.referenceInputsOfType<IbcState>())
                Handler.sendPacket(ctx, packet)
                ctx.verifyResults(tx.outputsOfType<IbcState>())
            }
        }
    }
}