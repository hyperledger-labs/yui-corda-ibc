package jp.datachain.corda.ibc.contracts.test

import com.google.protobuf.Any
import ibc.core.client.v1.Tx.MsgCreateClient
import ibc.lightclients.corda.v1.Corda
import ibc.lightclients.corda.v1.Corda.ConsensusState
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics26.Context
import jp.datachain.corda.ibc.ics26.HandleClientCreate
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.*
import net.corda.testing.node.MockServices
import net.corda.testing.node.internal.setDriverSerialization
import net.corda.testing.node.ledger
import org.junit.Test
import java.security.PublicKey
import kotlin.test.assertEquals

class IbcContractTests {
    private val alice = TestIdentity.fresh("Alice")
    private val bob = TestIdentity.fresh("Bob")
    private val bank = TestIdentity.fresh("Bank")
    private val relayer = TestIdentity.fresh("Relayer")
    private val ledgerServices = MockServices(
            listOf("jp.datachain.corda.ibc"),
            alice, bob, bank, relayer
    )

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createHost(signers: List<PublicKey>, participants: List<Party>) {
        val attachment = ledgerServices.attachments.getLatestContractAttachments(Ibc::class.qualifiedName!!).single()

        transaction {
            command(signers, Ibc.Commands.GenesisCreate())
            attachment(attachment)
            output(Ibc::class.qualifiedName!!, "genesis", Genesis(participants))
            verifies()
        }

        val genesisAndRef = "genesis".outputStateAndRef<Genesis>()

        transaction {
            command(signers, Ibc.Commands.HostCreate())
            input(genesisAndRef.ref)
            output(Ibc::class.qualifiedName!!, "host", Host(genesisAndRef))
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createCordaClient(signers: List<PublicKey>, msg: MsgCreateClient) {
        val hostAndRef = "host".outputStateAndRef<Host>()
        val ctx = Context(listOf(hostAndRef.state.data), emptyList()).also {
            val handler = HandleClientCreate(msg)
            handler.execute(it, signers)
        }
        assertEquals(ctx.outStates.size, 2)

        transaction {
            command(signers, HandleClientCreate(msg))
            input(hostAndRef.ref)
            ctx.outStates.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, "newHost", it)
                    is ClientState -> output(Ibc::class.qualifiedName!!, "client", it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }
    }

    @Test
    fun testIbc() {
        setDriverSerialization(ClassLoader.getSystemClassLoader()).use {
            ledgerServices.ledger {
                createHost(listOf(alice.publicKey), listOf(alice, bob, bank, relayer).map { it.party })

                val msg = MsgCreateClient.newBuilder()
                        .setClientState(Any.pack(Corda.ClientState.newBuilder().setId("corda-ibc-0").build(), ""))
                        .setConsensusState(Any.pack(ConsensusState.getDefaultInstance()))
                        .build()
                createCordaClient(listOf(alice.publicKey), msg)
            }
        }
    }
}