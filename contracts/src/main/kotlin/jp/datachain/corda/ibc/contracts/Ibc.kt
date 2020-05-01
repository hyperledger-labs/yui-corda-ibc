package jp.datachain.corda.ibc.contracts

import jp.datachain.corda.ibc.ics2.*
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.HostSeed
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics25.Handler.connOpenAck
import jp.datachain.corda.ibc.ics25.Handler.connOpenConfirm
import jp.datachain.corda.ibc.ics25.Handler.connOpenInit
import jp.datachain.corda.ibc.ics25.Handler.connOpenTry
import jp.datachain.corda.ibc.ics25.Handler.createClient
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class Ibc : Contract {
    override fun verify(tx: LedgerTransaction) = tx.commandsOfType<Commands>().single().value.verify(tx)

    interface Commands : CommandData {
        fun verify(tx: LedgerTransaction)

        class HostSeedCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "No state should be consumed" using (tx.inputs.size == 0)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                "Output type should be HostIdentifier" using (tx.outputs.single().data is HostSeed)
            }
        }

        class HostCreate : TypeOnlyCommandData(), Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val seed = tx.inRefsOfType<HostSeed>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val expected = Host(seed, newHost.linearId.id)
                "Output should be expected state" using (newHost == expected)
            }
        }

        data class ClientCreate(val clientType: ClientType, val consensusState: ConsensusState) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly two states should be created" using (tx.outputs.size == 2)
                val host = tx.inputsOfType<Host>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = host.createClient(newClient.id, clientType, consensusState)
                "Outputs should be expected states" using (Pair(newHost, newClient) == expected)
            }
        }

        data class ClientUpdate(val header: Header) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val client = tx.inputsOfType<ClientState>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = client.checkValidityAndUpdateState(header)
                "Output should be expected state" using (newClient ==  expected)
            }
        }

        data class ClientMisbehaviour(val evidence: Evidence) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val client = tx.inputsOfType<ClientState>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val expected = client.checkMisbehaviourAndUpdateState(evidence)
                "Output should be expected state" using (newClient ==  expected)
            }
        }

        data class ConnOpenInit(
                val desiredConnectionIdentifier: Identifier,
                val counterpartyPrefix: CommitmentPrefix,
                val clientIdentifier: Identifier,
                val counterpartyClientIdentifier: Identifier
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly two states should be consumed" using (tx.inputs.size == 2)
                "Exactly three states should be created" using (tx.outputs.size == 3)
                val host = tx.inputsOfType<Host>().single()
                val client = tx.inputsOfType<ClientState>().single()
                val newHost = tx.outputsOfType<Host>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val newConn = tx.outputsOfType<Connection>().single()
                val expected = Pair(host, client).connOpenInit(newConn.id, desiredConnectionIdentifier, counterpartyPrefix, clientIdentifier, counterpartyClientIdentifier)
                "Outputs should be expected states" using (Triple(newHost, newClient, newConn) == expected)
            }
        }

        data class ConnOpenTry(
                val desiredIdentifier: Identifier,
                val counterpartyConnectionIdentifier: Identifier,
                val counterpartyPrefix: CommitmentPrefix,
                val counterpartyClientIdentifier: Identifier,
                val clientIdentifier: Identifier,
                val counterpartyVersions: Version.Multiple,
                val proofInit: CommitmentProof,
                val proofConsensus: CommitmentProof,
                val proofHeight: Height,
                val consensusHeight: Height
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Two or three states should be consumed" using (tx.inputs.size == 2 || tx.inputs.size == 3)
                "Exactly three states should be created" using (tx.outputs.size == 3)
                val host = tx.inputsOfType<Host>().single()
                val client = tx.inputsOfType<ClientState>().single()
                val conn = if (tx.inputs.size == 3) {
                    tx.inputsOfType<Connection>().single()
                } else {
                    null
                }
                val newHost = tx.outputsOfType<Host>().single()
                val newClient = tx.outputsOfType<ClientState>().single()
                val newConn = tx.outputsOfType<Connection>().single()
                val expected = Triple(host, client, conn).connOpenTry(
                        desiredIdentifier,
                        counterpartyConnectionIdentifier,
                        counterpartyPrefix,
                        counterpartyClientIdentifier,
                        clientIdentifier,
                        counterpartyVersions,
                        proofInit,
                        proofConsensus,
                        proofHeight,
                        consensusHeight)
                "Outputs should be expected states" using (Triple(newHost, newClient, newConn) ==  expected)
            }
        }

        data class ConnOpenAck(
                val identifier: Identifier,
                val version: Version.Single,
                val proofTry: CommitmentProof,
                val proofConsensus: CommitmentProof,
                val proofHeight: Height,
                val consensusHeight: Height
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly two states should be referenced" using (tx.references.size == 2)
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val host = tx.referenceInputsOfType<Host>().single()
                val client = tx.referenceInputsOfType<ClientState>().single()
                val conn = tx.inputsOfType<Connection>().single()
                val newConn = tx.outputsOfType<Connection>().single()
                val expected = Triple(host, client, conn).connOpenAck(
                        identifier,
                        version,
                        proofTry,
                        proofConsensus,
                        proofHeight,
                        consensusHeight)
                "Output should be expected state" using (newConn ==  expected)
            }
        }

        data class ConnOpenConfirm(
                val identifier: Identifier,
                val proofAck: CommitmentProof,
                val proofHeight: Height
        ) : Commands {
            override fun verify(tx: LedgerTransaction) = requireThat {
                "Exactly two states should be referenced" using (tx.references.size == 2)
                "Exactly one state should be consumed" using (tx.inputs.size == 1)
                "Exactly one state should be created" using (tx.outputs.size == 1)
                val host = tx.referenceInputsOfType<Host>().single()
                val client = tx.referenceInputsOfType<ClientState>().single()
                val conn = tx.inputsOfType<Connection>().single()
                val newConn = tx.outputsOfType<Connection>().single()
                val expected = Triple(host, client, conn).connOpenConfirm(
                        identifier,
                        proofAck,
                        proofHeight)
                "Output should be expected state" using (newConn ==  expected)
            }
        }

        /*
        class ChanOpenInit : TypeOnlyCommandData(), Commands
        class ChanOpenTry : TypeOnlyCommandData(), Commands
        class ChanOpenAck : TypeOnlyCommandData(), Commands
        class ChanOpenConfirm : TypeOnlyCommandData(), Commands
        class ChanCloseInit : TypeOnlyCommandData(), Commands
        class ChanCloseConfirm : TypeOnlyCommandData(), Commands
        */
    }
}