package jp.datachain.corda.ibc.contracts.test

import com.google.protobuf.Any
import ibc.core.client.v1.Tx.MsgCreateClient
import ibc.core.connection.v1.Tx
import ibc.core.connection.v1.Tx.MsgConnectionOpenAck
import ibc.core.connection.v1.Tx.MsgConnectionOpenConfirm
import ibc.core.connection.v1.Tx.MsgConnectionOpenInit
import ibc.lightclients.corda.v1.Corda
import jp.datachain.corda.ibc.clients.corda.CordaClientState
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics26.*
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.sign
import net.corda.core.identity.Party
import net.corda.core.node.NotaryInfo
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.*
import net.corda.testing.node.MockServices
import net.corda.testing.node.internal.setDriverSerialization
import net.corda.testing.node.ledger
import org.junit.Test
import kotlin.test.assertEquals

class IbcContractTests {
    // players on chain A
    private val alice = TestIdentity.fresh("Alice")
    private val bankA = TestIdentity.fresh("BankA")
    private val notaryA = TestIdentity.fresh("NotaryA")
    // players on chain B
    private val bob = TestIdentity.fresh("Bob")
    private val bankB = TestIdentity.fresh("BankB")
    private val notaryB = TestIdentity.fresh("NotaryB")
    // a player working on both sides
    private val relayer = TestIdentity.fresh("Relayer")

    private val ledgerServices = MockServices(
            listOf("jp.datachain.corda.ibc"),
            relayer,
            testNetworkParameters(
                    notaries = listOf(notaryA, notaryB).map{ NotaryInfo(it.party, validating = true) },
                    minimumPlatformVersion = 4
            ), // The feature of reference state is supported since the platform version 4
            alice, bob, bankA, bankB, notaryA, notaryB
    )

    interface StateType
    object GENESIS : StateType
    object HOST : StateType
    object CLIENT : StateType
    object CONNECTION : StateType
    object CHANNEL : StateType

    interface ChainType
    object A : ChainType
    object B : ChainType

    private val seqMap = mutableMapOf<String, Long>()
    private fun concatTypes(s: StateType, c: ChainType) = "${s::class.simpleName}${c::class.simpleName}"
    private fun label(s: StateType, c: ChainType) = "${concatTypes(s, c)}${seqMap[concatTypes(s, c)]!!}"
    private fun newLabel(s: StateType, c: ChainType): String {
        val cat = concatTypes(s, c)
        seqMap[cat] = seqMap.getOrDefault(cat, 0) + 1
        return label(s, c)
    }

    private fun notaryOf(chain: ChainType): TestIdentity = when(chain) {
        is A -> notaryA
        is B -> notaryB
        else -> throw IllegalArgumentException("chain must be A or B")
    }

    private fun signTx(wtx: WireTransaction, vararg identities: TestIdentity): SignedTransaction {
        val signatureScheme = identities.map{Crypto.findSignatureScheme(it.publicKey)}.distinct().single()
        val signableData = SignableData(wtx.id, SignatureMetadata(ledgerServices.myInfo.platformVersion, signatureScheme.schemeNumberID))
        val sigs = identities.map{it.keyPair.sign(signableData)}
        return SignedTransaction(wtx, sigs)
    }

    companion object {
        private const val CLIENT_ID = "corda-ibc-0"
        private const val CONNECTION_ID = "connection-0"
        private const val PORT_ID = "transfer"
        private const val CHANNEL_ID = "channel-0"
        private const val CHANNEL_VERSION_A = "CHANNEL_VERSION_A"
        private const val CHANNEL_VERSION_B = "CHANNEL_VERSION_B"
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.transactionOn(
            chain: ChainType,
            dsl: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail
    ) = signTx(transaction(transactionBuilder = TransactionBuilder(notaryOf(chain).party), dsl = dsl), relayer, notaryOf(chain))

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createHost(participants: List<Party>, chain: ChainType) {
        val attachmentId = ledgerServices.attachments.getLatestContractAttachments(Ibc::class.qualifiedName!!).single()

        transactionOn(chain) {
            command(listOf(relayer.publicKey), Ibc.Commands.GenesisCreate())
            attachment(attachmentId)
            output(Ibc::class.qualifiedName!!, newLabel(GENESIS, chain), Genesis(participants))
            verifies()
        }

        val genesis = label(GENESIS, chain).outputStateAndRef<Genesis>()

        transactionOn(chain) {
            command(listOf(relayer.publicKey), Ibc.Commands.HostCreate())
            input(genesis.ref)
            output(Ibc::class.qualifiedName!!, newLabel(HOST, chain), Host(genesis))
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createCordaClient(self: ChainType, counterparty: ChainType): SignedTransaction {
        val host = label(HOST, self).outputStateAndRef<Host>()
        val counterpartyHost = label(HOST, counterparty).outputStateAndRef<Host>()
        val msg = MsgCreateClient.newBuilder()
                .setClientState(Any.pack(Corda.ClientState.newBuilder().setId(CLIENT_ID).build(), ""))
                .setConsensusState(counterpartyHost.state.data.let{it.getConsensusState(it.getCurrentHeight())}.consensusState)
                .build()
        val handler = HandleClientCreate(msg)
        val ctx = Context(listOf(host.state.data), emptyList()).also {
            handler.execute(it, listOf(relayer.publicKey))
        }
        assertEquals(ctx.outStates.size, 2)

        return transactionOn(self) {
            command(listOf(relayer.publicKey), HandleClientCreate(msg))
            input(host.ref)
            ctx.outStates.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, newLabel(HOST, self), it)
                    is ClientState -> output(Ibc::class.qualifiedName!!, newLabel(CLIENT, self), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createConnection(
            stxClientA: SignedTransaction,
            stxClientB: SignedTransaction
    ): Pair<SignedTransaction, SignedTransaction> {
        var hostA = label(HOST, A).outputStateAndRef<Host>()
        var hostB = label(HOST, B).outputStateAndRef<Host>()
        val clientA = label(CLIENT, A).outputStateAndRef<CordaClientState>() // CordaClientState is constant
        val prefixA = hostA.state.data.getCommitmentPrefix()
        val prefixB = hostB.state.data.getCommitmentPrefix()
        val versionsA = hostA.state.data.getCompatibleVersions()

        val stxConnInit = transactionOn(A) {
            val handler = HandleConnOpenInit(MsgConnectionOpenInit.newBuilder().apply{
                clientId = CLIENT_ID
                counterpartyBuilder.clientId = CLIENT_ID
                counterpartyBuilder.connectionId = ""
                counterpartyBuilder.prefix = prefixB
                version = versionsA.single()
                delayPeriod = 0
            }.build())
            val outputs = Context(listOf(hostA.state.data), listOf(clientA.state.data)).also{
                handler.execute(it, listOf(relayer.publicKey))
            }.outStates
            assertEquals(outputs.size, 2)

            command(listOf(relayer.publicKey), handler)
            input(hostA.ref)
            reference(clientA.ref)
            outputs.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, newLabel(HOST, A), it)
                    is IbcConnection -> output(Ibc::class.qualifiedName!!, newLabel(CONNECTION, A), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        hostA = label(HOST, A).outputStateAndRef()
        val clientB = label(CLIENT, B).outputStateAndRef<CordaClientState>() // CordaClientState is constant
        var connA = label(CONNECTION, A).outputStateAndRef<IbcConnection>()

        val stxConnTry = transactionOn(B) {
            val handler = HandleConnOpenTry(Tx.MsgConnectionOpenTry.newBuilder().apply{
                clientId = CLIENT_ID
                clientState = clientA.state.data.clientState
                counterpartyBuilder.clientId = CLIENT_ID
                counterpartyBuilder.connectionId = CONNECTION_ID
                counterpartyBuilder.prefix = prefixA
                delayPeriod = 0
                addAllCounterpartyVersions(versionsA)
                proofHeight = hostA.state.data.getCurrentHeight()
                proofInit = stxConnInit.toProof().toByteString()
                proofClient = stxClientA.toProof().toByteString()
                proofConsensus = stxClientA.toProof().toByteString()
                consensusHeight = hostB.state.data.getCurrentHeight()
            }.build())
            val outputs = Context(listOf(hostB.state.data), listOf(clientB.state.data))
                    .also{ handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 2)

            command(listOf(relayer.publicKey), handler)
            input(hostB.ref)
            reference(clientB.ref)
            outputs.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, newLabel(HOST, B), it)
                    is IbcConnection -> output(Ibc::class.qualifiedName!!, newLabel(CONNECTION, B), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        hostB = label(HOST, B).outputStateAndRef()
        val connB = label(CONNECTION, B).outputStateAndRef<IbcConnection>()

        val stxConnAck = transactionOn(A) {
            val handler = HandleConnOpenAck(MsgConnectionOpenAck.newBuilder().apply{
                connectionId = CONNECTION_ID
                counterpartyConnectionId = CONNECTION_ID
                version = versionsA.single()
                clientState = clientB.state.data.clientState
                proofHeight = hostB.state.data.getCurrentHeight()
                proofTry = stxConnTry.toProof().toByteString()
                proofClient = stxClientB.toProof().toByteString()
                proofConsensus = stxClientB.toProof().toByteString()
                consensusHeight = hostA.state.data.getCurrentHeight()
            }.build())
            val outputs = Context(listOf(connA.state.data), listOf(hostA.state.data, clientA.state.data))
                    .also{ handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 1)

            command(listOf(relayer.publicKey), handler)
            input(connA.ref)
            reference(hostA.ref)
            reference(clientA.ref)
            output(Ibc::class.qualifiedName!!, newLabel(CONNECTION, A), outputs.single())
            verifies()
        }

        connA = label(CONNECTION, A).outputStateAndRef()

        val stxConnConfirm = transactionOn(B) {
            val handler = HandleConnOpenConfirm(MsgConnectionOpenConfirm.newBuilder().apply{
                connectionId = CONNECTION_ID
                proofAck = stxConnAck.toProof().toByteString()
                proofHeight = hostA.state.data.getCurrentHeight()
            }.build())
            val outputs = Context(listOf(connB.state.data), listOf(hostB.state.data, clientB.state.data))
                    .also{ handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 1)

            command(listOf(relayer.publicKey), handler)
            input(connB.ref)
            reference(hostB.ref)
            reference(clientB.ref)
            output(Ibc::class.qualifiedName!!, newLabel(CONNECTION, B), outputs.single())
            verifies()
        }

        return Pair(stxConnAck, stxConnConfirm)
    }

    @Test
    fun testIbc() {
        setDriverSerialization(ClassLoader.getSystemClassLoader()).use {
            ledgerServices.ledger {
                createHost(listOf(alice, bankA, relayer).map { it.party }, A)
                createHost(listOf(bob, bankB, relayer).map { it.party }, B)

                val stxClientA = createCordaClient(A, B)
                val stxClientB = createCordaClient(B, A)

                val (stxConnA, stxConnB) = createConnection(stxClientA, stxClientB)
            }
        }
    }
}