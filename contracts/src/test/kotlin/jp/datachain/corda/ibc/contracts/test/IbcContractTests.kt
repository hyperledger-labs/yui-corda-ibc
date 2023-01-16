package jp.datachain.corda.ibc.contracts.test

import ibc.applications.transfer.v1.Tx.MsgTransfer
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.channel.v1.Tx.*
import ibc.core.client.v1.Client
import ibc.core.client.v1.Tx.MsgCreateClient
import ibc.core.connection.v1.Tx.*
import ibc.lightclients.corda.v1.Corda
import ibc.lightclients.fabric.v1.Fabric
import jp.datachain.corda.ibc.clients.corda.CordaClientStateFactory
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.clients.fabric.FabricClientStateFactory
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.conversion.pack
import jp.datachain.corda.ibc.conversion.toProto
import jp.datachain.corda.ibc.ics20.Address
import jp.datachain.corda.ibc.ics20.Denom
import jp.datachain.corda.ibc.ics20.toJson
import jp.datachain.corda.ibc.ics20cash.CashBank
import jp.datachain.corda.ibc.ics20cash.Voucher
import jp.datachain.corda.ibc.ics24.Genesis
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics26.*
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcClientState
import jp.datachain.corda.ibc.states.IbcConnection
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.crypto.sign
import net.corda.core.identity.Party
import net.corda.core.node.NotaryInfo
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.AMOUNT
import net.corda.finance.JPY
import net.corda.finance.USD
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.`issued by`
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.*
import net.corda.testing.node.MockServices
import net.corda.testing.node.internal.setDriverSerialization
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.Currency
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
            listOf("jp.datachain.corda.ibc", "net.corda.finance.contracts.asset"),
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
    object CASHBANK : StateType
    object CASH : StateType
    object VOUCHER : StateType

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

    private fun userOf(chain: ChainType): TestIdentity = when(chain) {
        is A -> alice
        is B -> bob
        else -> throw IllegalArgumentException("chain must be A or B")
    }

    private fun bankOf(chain: ChainType): TestIdentity = when(chain) {
        is A -> bankA
        is B -> bankB
        else -> throw IllegalArgumentException("chain must be A or B")
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

    private val TestIdentity.partyAndReference get() = PartyAndReference(party, OpaqueBytes(ByteArray(1)))

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
            initiator: TestIdentity = relayer,
            dsl: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail
    ) = signTx(transaction(transactionBuilder = TransactionBuilder(notaryOf(chain).party), dsl = dsl), initiator, notaryOf(chain))

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createHost(participants: List<Party>, chain: ChainType) {
        transactionOn(chain) {
            command(relayer.publicKey, Ibc.MiscCommands.GenesisCreate())
            output(Ibc::class.qualifiedName!!, newLabel(GENESIS, chain), Genesis(participants))
            verifies()
        }

        val genesis = label(GENESIS, chain).outputStateAndRef<Genesis>()
        val moduleNames = mapOf(
                Identifier("nop") to NopModule::class.qualifiedName!!,
                Identifier("transfer") to jp.datachain.corda.ibc.ics20cash.Module::class.qualifiedName!!,
                Identifier("transfer-old") to jp.datachain.corda.ibc.ics20.Module::class.qualifiedName!!
        )
        val clientStateFactoryNames = mapOf(
                Corda.ClientState.getDescriptor().fullName to CordaClientStateFactory::class.qualifiedName!!,
                Fabric.ClientState.getDescriptor().fullName to FabricClientStateFactory::class.qualifiedName!!
        )

        transactionOn(chain) {
            command(relayer.publicKey, Ibc.MiscCommands.HostCreate(moduleNames, clientStateFactoryNames))
            input(genesis.ref)
            output(Ibc::class.qualifiedName!!, newLabel(HOST, chain), Host(genesis, moduleNames, clientStateFactoryNames))
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createCordaClient(self: ChainType, counterparty: ChainType): SignedTransaction {
        val host = label(HOST, self).outputStateAndRef<Host>()
        val counterpartyHost = label(HOST, counterparty).outputStateAndRef<Host>()
        val msg = MsgCreateClient.newBuilder()
                .setClientState(Corda.ClientState.newBuilder().apply {
                    baseId = counterpartyHost.state.data.baseId.toProto()
                    notaryKey = counterpartyHost.state.data.notary.owningKey.toProto()
                }.build().pack())
                .setConsensusState(counterpartyHost.state.data.let{it.getConsensusState(it.getCurrentHeight())}.anyConsensusState)
                .build()
        val handler = HandleClientCreate(msg)
        val ctx = Context(listOf(host.state.data), emptyList()).also {
            handler.execute(it, listOf(relayer.publicKey))
        }
        assertEquals(ctx.outStates.size, 2)

        return transactionOn(self) {
            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleClientCreate(HandleClientCreate(msg)))
            input(host.ref)
            ctx.outStates.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, newLabel(HOST, self), it)
                    is IbcClientState -> output(Ibc::class.qualifiedName!!, newLabel(CLIENT, self), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.establishConnection(
            stxClientA: SignedTransaction,
            stxClientB: SignedTransaction
    ) {
        var hostA = label(HOST, A).outputStateAndRef<Host>()
        var hostB = label(HOST, B).outputStateAndRef<Host>()
        val clientA = label(CLIENT, A).outputStateAndRef<IbcClientState>() // CordaClientState is constant
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

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleConnOpenInit(handler))
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
        val clientB = label(CLIENT, B).outputStateAndRef<IbcClientState>() // CordaClientState is constant
        var connA = label(CONNECTION, A).outputStateAndRef<IbcConnection>()

        val stxConnTry = transactionOn(B) {
            val handler = HandleConnOpenTry(MsgConnectionOpenTry.newBuilder().apply{
                clientId = CLIENT_ID
                clientState = clientA.state.data.anyClientState
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

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleConnOpenTry(handler))
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
                clientState = clientB.state.data.anyClientState
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

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleConnOpenAck(handler))
            input(connA.ref)
            reference(hostA.ref)
            reference(clientA.ref)
            output(Ibc::class.qualifiedName!!, newLabel(CONNECTION, A), outputs.single())
            verifies()
        }

        connA = label(CONNECTION, A).outputStateAndRef()

        @Suppress("UNUSED_VARIABLE")
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

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleConnOpenConfirm(handler))
            input(connB.ref)
            reference(hostB.ref)
            reference(clientB.ref)
            output(Ibc::class.qualifiedName!!, newLabel(CONNECTION, B), outputs.single())
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.establishChannel() {
        var hostA = label(HOST, A).outputStateAndRef<Host>()
        val connA = label(CONNECTION, A).outputStateAndRef<IbcConnection>() // CordaClientState is constant

        val stxChanInit = transactionOn(A) {
            val handler = HandleChanOpenInit(MsgChannelOpenInit.newBuilder().apply{
                portId = PORT_ID
                channelBuilder.ordering = ChannelOuterClass.Order.ORDER_ORDERED
                channelBuilder.counterpartyBuilder.portId = PORT_ID
                channelBuilder.counterpartyBuilder.channelId = ""
                channelBuilder.addAllConnectionHops(listOf(CONNECTION_ID))
                channelBuilder.version = CHANNEL_VERSION_A
            }.build())
            val inputs = listOf(hostA)
            val references = listOf(connA)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 2)

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleChanOpenInit(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, newLabel(HOST, A), it)
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, A), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        hostA = label(HOST, A).outputStateAndRef()
        var chanA = label(CHANNEL, A).outputStateAndRef<IbcChannel>()
        var hostB = label(HOST, B).outputStateAndRef<Host>()
        val clientB = label(CLIENT, B).outputStateAndRef<IbcClientState>()
        val connB = label(CONNECTION, B).outputStateAndRef<IbcConnection>()

        val stxChanTry = transactionOn(B) {
            val handler = HandleChanOpenTry(MsgChannelOpenTry.newBuilder().apply{
                portId = PORT_ID
                channelBuilder.ordering = ChannelOuterClass.Order.ORDER_ORDERED
                channelBuilder.counterpartyBuilder.portId = PORT_ID
                channelBuilder.counterpartyBuilder.channelId = CHANNEL_ID
                channelBuilder.addAllConnectionHops(listOf(CONNECTION_ID))
                channelBuilder.version = CHANNEL_VERSION_B
                counterpartyVersion = CHANNEL_VERSION_A
                proofInit = stxChanInit.toProof().toByteString()
                proofHeight = hostA.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(hostB)
            val references = listOf(clientB, connB)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 2)

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleChanOpenTry(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach{
                when(it) {
                    is Host -> output(Ibc::class.qualifiedName!!, newLabel(HOST, B), it)
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, B), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        hostB = label(HOST, B).outputStateAndRef()
        val chanB = label(CHANNEL, B).outputStateAndRef<IbcChannel>()
        val clientA = label(CLIENT, A).outputStateAndRef<IbcClientState>()

        val stxChanAck = transactionOn(A) {
            val handler = HandleChanOpenAck(MsgChannelOpenAck.newBuilder().apply{
                portId = PORT_ID
                channelId = CHANNEL_ID
                counterpartyChannelId = CHANNEL_ID
                counterpartyVersion = CHANNEL_VERSION_B
                proofTry = stxChanTry.toProof().toByteString()
                proofHeight = hostB.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(chanA)
            val references = listOf(hostA, clientA, connA)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 1)

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleChanOpenAck(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, A), outputs.single())
            verifies()
        }

        hostA = label(HOST, A).outputStateAndRef()
        chanA = label(CHANNEL, A).outputStateAndRef()

        @Suppress("UNUSED_VARIABLE")
        val stxChanConfirm = transactionOn(B) {
            val handler = HandleChanOpenConfirm(MsgChannelOpenConfirm.newBuilder().apply{
                portId = PORT_ID
                channelId = CHANNEL_ID
                proofAck = stxChanAck.toProof().toByteString()
                proofHeight = hostA.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(chanB)
            val references = listOf(hostB, clientB, connB)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(outputs.size, 1)

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandleChanOpenConfirm(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, B), outputs.single())
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.createCashBank(chain: ChainType) {
        val bankIdentity = bankOf(chain)
        val host = label(HOST, chain).outputStateAndRef<Host>()

        transactionOn(chain) {
            val handler = Ibc.MiscCommands.CashBankCreate(bankIdentity.party)
            val expectedBank = CashBank(host.state.data, bankIdentity.party)
            val expectedHost = host.state.data.addBank(expectedBank.id)

            command(bankIdentity.publicKey, handler)
            input(host.ref)
            output(Ibc::class.qualifiedName!!, newLabel(HOST, chain), expectedHost)
            output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, chain), expectedBank)
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.mintCash(currency: Currency, quantity: Long, chain: ChainType) {
        val bankIdentity = bankOf(chain)
        val userIdentity = userOf(chain)
        val amount = AMOUNT(quantity, currency) `issued by` bankIdentity.partyAndReference

        transactionOn(chain) {
            command(bankIdentity.publicKey, Cash.Commands.Issue())
            output(Cash.PROGRAM_ID, newLabel(CASH, chain), Cash.State(amount, bankIdentity.party))
            verifies()
        }

        transactionOn(chain) {
            command(bankIdentity.publicKey, Cash.Commands.Move())
            input(label(CASH, chain))
            output(Cash.PROGRAM_ID, newLabel(CASH, chain), Cash.State(amount, userIdentity.party))
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.transferCash(senderChain: ChainType, receiverChain: ChainType) {
        val senderIdentity = userOf(senderChain)
        val receiverIdentity = userOf(receiverChain)

        val hostS = label(HOST, senderChain).outputStateAndRef<Host>()
        val clientS = label(CLIENT, senderChain).outputStateAndRef<IbcClientState>()
        val connS = label(CONNECTION, senderChain).outputStateAndRef<IbcConnection>()

        var cashBankS = label(CASHBANK, senderChain).outputStateAndRef<CashBank>()
        var chanS = label(CHANNEL, senderChain).outputStateAndRef<IbcChannel>()
        var cashS = label(CASH, senderChain).outputStateAndRef<Cash.State>()
        val denom = cashS.state.data.amount.token.let { Denom.fromIssuedCurrency(it.issuer.party.owningKey, it.product) }
        val amount = cashS.state.data.amount.toDecimal().longValueExact()

        val stxTransfer = transactionOn(chain = senderChain, initiator = senderIdentity) {
            val handler = CreateOutgoingPacket(MsgTransfer.newBuilder().apply{
                sourcePort = PORT_ID
                sourceChannel = CHANNEL_ID
                tokenBuilder.denom = denom.toString()
                tokenBuilder.amount = amount.toString()
                sender = Address.fromPublicKey(senderIdentity.publicKey).toBech32()
                receiver = Address.fromPublicKey(receiverIdentity.publicKey).toBech32()
                timeoutHeight = Client.Height.getDefaultInstance()
                timeoutTimestamp = 0
            }.build().pack())
            val inputs = listOf(chanS, cashBankS, cashS)
            val references = listOf(hostS, clientS, connS)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(senderIdentity.publicKey)) }
                    .outStates
            assertEquals(expected = 3, actual = outputs.size)

            command(senderIdentity.publicKey, Ibc.DatagramHandlerCommand.CreateOutgoingPacket(handler))
            command(senderIdentity.publicKey, Cash.Commands.Move())
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach {
                when (it) {
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, senderChain), it)
                    is CashBank -> output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, senderChain), it)
                    is Cash.State -> output(Cash.PROGRAM_ID, newLabel(CASH, senderChain), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        chanS = label(CHANNEL, senderChain).outputStateAndRef()
        cashBankS = label(CASHBANK, senderChain).outputStateAndRef()
        cashS = label(CASH, senderChain).outputStateAndRef()
        val packetData = chanS.state.data.let { it.packets[it.nextSequenceSend - 1]!! }

        val hostR = label(HOST, receiverChain).outputStateAndRef<Host>()
        val clientR = label(CLIENT, receiverChain).outputStateAndRef<IbcClientState>()
        val connR = label(CONNECTION, receiverChain).outputStateAndRef<IbcConnection>()

        var chanR = label(CHANNEL, receiverChain).outputStateAndRef<IbcChannel>()
        var cashBankR = label(CASHBANK, receiverChain).outputStateAndRef<CashBank>()

        val stxRecv = transactionOn(receiverChain) {
            val handler = HandlePacketRecv(MsgRecvPacket.newBuilder().apply{
                packet = packetData
                proofCommitment = stxTransfer.toProof().toByteString()
                proofHeight = hostS.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(chanR, cashBankR)
            val references = listOf(hostR, clientR, connR)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(expected = 3, actual = outputs.size)

            command(relayer.publicKey, Ibc.DatagramHandlerCommand.HandlePacketRecv(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach {
                when (it) {
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, receiverChain), it)
                    is CashBank -> output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, receiverChain), it)
                    is Voucher -> output(Ibc::class.qualifiedName!!, newLabel(VOUCHER, receiverChain), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        chanR = label(CHANNEL, receiverChain).outputStateAndRef()
        cashBankR = label(CASHBANK, receiverChain).outputStateAndRef()
        val ackData = chanR.state.data.let { it.acknowledgements[packetData.sequence]!! }

        transactionOn(senderChain) {
            val handler = HandlePacketAcknowledgement(MsgAcknowledgement.newBuilder().apply {
                packet = packetData
                acknowledgement = ackData.toJson()
                proofAcked = stxRecv.toProof().toByteString()
                proofHeight = hostR.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(chanS, cashBankS)
            val references = listOf(hostS, clientS, connS)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { ctx -> handler.execute(ctx, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(expected = 2, actual = outputs.size)

            command(listOf(relayer.publicKey), Ibc.DatagramHandlerCommand.HandlePacketAcknowledgement(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach{
                when(it) {
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, senderChain), it)
                    is CashBank -> output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, senderChain), it)
                    else -> IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }
    }

    private fun LedgerDSL<TestTransactionDSLInterpreter, TestLedgerDSLInterpreter>.transferCashBack(senderChain: ChainType, receiverChain: ChainType) {
        val senderIdentity = userOf(senderChain)
        val receiverIdentity = userOf(receiverChain)
        val receiverBankIdentity = bankOf(receiverChain)

        val hostS = label(HOST, senderChain).outputStateAndRef<Host>()
        val clientS = label(CLIENT, senderChain).outputStateAndRef<IbcClientState>()
        val connS = label(CONNECTION, senderChain).outputStateAndRef<IbcConnection>()

        var cashBankS = label(CASHBANK, senderChain).outputStateAndRef<CashBank>()
        var chanS = label(CHANNEL, senderChain).outputStateAndRef<IbcChannel>()
        val voucherS = label(VOUCHER, senderChain).outputStateAndRef<Voucher>()
        val denom = voucherS.state.data.amount.token.let(::Denom)
        val amount = voucherS.state.data.amount.toDecimal().longValueExact()

        val stxTransfer = transactionOn(chain = senderChain, initiator = senderIdentity) {
            val handler = CreateOutgoingPacket(MsgTransfer.newBuilder().apply{
                sourcePort = PORT_ID
                sourceChannel = CHANNEL_ID
                tokenBuilder.denom = denom.toIbcDenom()
                tokenBuilder.amount = amount.toString()
                sender = Address.fromPublicKey(senderIdentity.publicKey).toBech32()
                receiver = Address.fromPublicKey(receiverIdentity.publicKey).toBech32()
                timeoutHeight = Client.Height.getDefaultInstance()
                timeoutTimestamp = 0
            }.build().pack())
            val inputs = listOf(chanS, cashBankS, voucherS)
            val references = listOf(hostS, clientS, connS)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(senderIdentity.publicKey)) }
                    .outStates
            assertEquals(expected = 2, actual = outputs.size)

            command(senderIdentity.publicKey, Ibc.DatagramHandlerCommand.CreateOutgoingPacket(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach {
                when (it) {
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, senderChain), it)
                    is CashBank -> output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, senderChain), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        chanS = label(CHANNEL, senderChain).outputStateAndRef()
        cashBankS = label(CASHBANK, senderChain).outputStateAndRef()
        val packetData = chanS.state.data.let { it.packets[it.nextSequenceSend - 1]!! }

        val hostR = label(HOST, receiverChain).outputStateAndRef<Host>()
        val clientR = label(CLIENT, receiverChain).outputStateAndRef<IbcClientState>()
        val connR = label(CONNECTION, receiverChain).outputStateAndRef<IbcConnection>()

        var chanR = label(CHANNEL, receiverChain).outputStateAndRef<IbcChannel>()
        var cashBankR = label(CASHBANK, receiverChain).outputStateAndRef<CashBank>()
        var cashR = label(CASH, receiverChain).outputStateAndRef<Cash.State>()

        val stxRecv = transactionOn(chain = receiverChain, initiator = receiverBankIdentity) {
            val handler = HandlePacketRecv(MsgRecvPacket.newBuilder().apply{
                packet = packetData
                proofCommitment = stxTransfer.toProof().toByteString()
                proofHeight = hostS.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(chanR, cashBankR, cashR)
            val references = listOf(hostR, clientR, connR)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { handler.execute(it, listOf(receiverBankIdentity.publicKey)) }
                    .outStates
            assertEquals(expected = 3, actual = outputs.size)

            command(receiverBankIdentity.publicKey, Ibc.DatagramHandlerCommand.HandlePacketRecv(handler))
            command(receiverBankIdentity.publicKey, Cash.Commands.Move())
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach {
                when (it) {
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, receiverChain), it)
                    is CashBank -> output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, receiverChain), it)
                    is Cash.State -> output(Cash.PROGRAM_ID, newLabel(CASH, receiverChain), it)
                    else -> throw IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }

        chanR = label(CHANNEL, receiverChain).outputStateAndRef()
        cashBankR = label(CASHBANK, receiverChain).outputStateAndRef()
        cashR = label(CASH, receiverChain).outputStateAndRef()
        val ackData = chanR.state.data.let { it.acknowledgements[packetData.sequence]!! }

        transactionOn(senderChain) {
            val handler = HandlePacketAcknowledgement(MsgAcknowledgement.newBuilder().apply {
                packet = packetData
                acknowledgement = ackData.toJson()
                proofAcked = stxRecv.toProof().toByteString()
                proofHeight = hostR.state.data.getCurrentHeight()
            }.build())
            val inputs = listOf(chanS, cashBankS)
            val references = listOf(hostS, clientS, connS)
            val outputs = Context(inputs.map{it.state.data}, references.map{it.state.data})
                    .also { ctx -> handler.execute(ctx, listOf(relayer.publicKey)) }
                    .outStates
            assertEquals(expected = 2, actual = outputs.size)

            command(listOf(relayer.publicKey), Ibc.DatagramHandlerCommand.HandlePacketAcknowledgement(handler))
            inputs.forEach{input(it.ref)}
            references.forEach{reference(it.ref)}
            outputs.forEach{
                when(it) {
                    is IbcChannel -> output(Ibc::class.qualifiedName!!, newLabel(CHANNEL, senderChain), it)
                    is CashBank -> output(Ibc::class.qualifiedName!!, newLabel(CASHBANK, senderChain), it)
                    else -> IllegalArgumentException("unexpected output state")
                }
            }
            verifies()
        }
    }

    @Test
    fun testIbc() {
        setDriverSerialization(ClassLoader.getSystemClassLoader()).use {
            ledgerServices.ledger {
                // create host state on both chains
                createHost(listOf(alice, bankA, relayer).map { it.party }, A)
                createHost(listOf(bob, bankB, relayer).map { it.party }, B)

                // create client states on both chains
                val stxClientA = createCordaClient(A, B)
                val stxClientB = createCordaClient(B, A)

                // establish a connection between chain A and B
                establishConnection(stxClientA, stxClientB)

                // establish a channel between chain A and B
                establishChannel()

                // create banks for ics20cash on both chains
                createCashBank(A)
                createCashBank(B)

                // mint some money to Alice
                mintCash(JPY, 100, A)
                // mint some money to Bob
                mintCash(USD, 100, B)

                // transfer Cash (100 JPY) from Alice to Bob
                transferCash(A, B)
                // transfer Cash (100 USD) from Bob to Alice
                transferCash(B, A)

                // transfer Cash (100 JPY) back from Bob to Alice
                transferCashBack(B, A)
                // transfer Cash (100 USD) back from Alice to Bob
                transferCashBack(A, B)
            }
        }
    }
}