package jp.datachain.corda.ibc.contracts

import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics20.Amount
import jp.datachain.corda.ibc.ics20.Bank
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.acknowledgePacket
import jp.datachain.corda.ibc.ics25.Handler.chanCloseConfirm
import jp.datachain.corda.ibc.ics25.Handler.recvPacket
import jp.datachain.corda.ibc.ics25.Handler.sendPacket
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.DatagramHandler
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.states.Channel
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.states.IbcState
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Quadruple
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

        data class ChanCloseConfirm(
                val portIdentifier: Identifier,
                val channelIdentifier: Identifier,
                val proofInit: CommitmentProof,
                val proofHeight: Height
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly three states should be referenced" using (tx.references.size == 3)
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val host = tx.referenceInputsOfType<Host>().single()
                val client = tx.referenceInputsOfType<ClientState>().single()
                val conn = tx.referenceInputsOfType<Connection>().single()
                val chan = tx.inputsOfType<Channel>().single()
                val newChan = tx.outputsOfType<Channel>().single()
                val expected = Quadruple(host, client, conn, chan).chanCloseConfirm(
                        portIdentifier,
                        channelIdentifier,
                        proofInit,
                        proofHeight)
                "Output should be expected state: ${newChan} != ${expected}" using (newChan == expected)
            }
        }

        data class SendPacket(val packet: Packet) : Commands {
            override fun verify(tx: LedgerTransaction) {
                val ctx = Context(tx.inputsOfType<IbcState>(), tx.referenceInputsOfType<IbcState>())
                sendPacket(ctx, packet)
                ctx.verifyResults(tx.outputsOfType<IbcState>())
            }
        }

        data class RecvPacket(
                val packet: Packet,
                val proof: CommitmentProof,
                val proofHeight: Height,
                val acknowledgement: Acknowledgement
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly three states should be referenced" using (tx.references.size == 3)
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val host = tx.referenceInputsOfType<Host>().single()
                val client = tx.referenceInputsOfType<ClientState>().single()
                val conn = tx.referenceInputsOfType<Connection>().single()
                val chan = tx.inputsOfType<Channel>().single()
                val newChan = tx.outputsOfType<Channel>().single()
                val expected = Quadruple(host, client, conn, chan).recvPacket(
                        packet,
                        proof,
                        proofHeight,
                        acknowledgement)
                "Output should be expected state: ${newChan} != ${expected}" using (newChan == expected)
            }
        }

        data class AcknowledgePacket(
                val packet: Packet,
                val acknowledgement: Acknowledgement,
                val proof: CommitmentProof,
                val proofHeight: Height
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly three states should be referenced" using (tx.references.size == 3)
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val host = tx.referenceInputsOfType<Host>().single()
                val client = tx.referenceInputsOfType<ClientState>().single()
                val conn = tx.referenceInputsOfType<Connection>().single()
                val chan = tx.inputsOfType<Channel>().single()
                val newChan = tx.outputsOfType<Channel>().single()
                val expected = Quadruple(host, client, conn, chan).acknowledgePacket(
                        packet,
                        acknowledgement,
                        proof,
                        proofHeight)
                "Output should be expected state: ${newChan} != ${expected}" using (newChan == expected)
            }
        }
    }
}