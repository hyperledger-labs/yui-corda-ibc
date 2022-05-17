package jp.datachain.corda.ibc

import ibc.applications.transfer.v1.Tx.*
import ibc.core.channel.v1.ChannelOuterClass
import ibc.core.channel.v1.Tx.*
import ibc.core.client.v1.Tx.*
import ibc.core.connection.v1.Connection
import ibc.core.connection.v1.Tx.*
import jp.datachain.corda.ibc.clients.corda.toProof
import jp.datachain.corda.ibc.flows.ics2.*
import jp.datachain.corda.ibc.flows.ics20.*
import jp.datachain.corda.ibc.flows.ics20cash.IbcCashBankCreateFlow
import jp.datachain.corda.ibc.flows.ics20cash.IbcTransferFlow
import jp.datachain.corda.ibc.flows.ics24.*
import jp.datachain.corda.ibc.flows.ics3.*
import jp.datachain.corda.ibc.flows.ics4.*
import jp.datachain.corda.ibc.flows.util.queryIbcBank
import jp.datachain.corda.ibc.flows.util.queryIbcCashBank
import jp.datachain.corda.ibc.flows.util.queryIbcHost
import jp.datachain.corda.ibc.flows.util.queryIbcState
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics20.*
import jp.datachain.corda.ibc.ics20cash.CashBank
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.states.IbcChannel
import jp.datachain.corda.ibc.states.IbcConnection
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.StateRef
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.AMOUNT
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import java.util.*

class TestCordaIbcClient(private val mockNet: MockNetwork, private val mockNode: StartedMockNode) {
    var maybeBaseId: StateRef? = null
    val baseId
        get() = maybeBaseId!!
    fun setBaseId(baseId: StateRef) {
        assert(maybeBaseId == null)
        maybeBaseId = baseId
    }

    fun host() = mockNode.services.vaultService.queryIbcHost(baseId)!!.state.data

    fun bank() = mockNode.services.vaultService.queryIbcBank(baseId)!!.state.data

    fun cashBank() = mockNode.services.vaultService.queryIbcCashBank(baseId)!!.state.data

    private inline fun <reified T: IbcState> queryStateWithProof(id: Identifier): Pair<T, CommitmentProof> {
        val stateAndRef = mockNode.services.vaultService.queryIbcState<T>(baseId, id)!!
        val stx = mockNode.services.validatedTransactions.getTransaction(stateAndRef.ref.txhash)!!
        val state = stateAndRef.state.data
        return Pair(state, stx.toProof())
    }

    fun client(id: Identifier) = queryStateWithProof<ClientState>(id).first
    fun clientProof(id: Identifier) = queryStateWithProof<ClientState>(id).second

    fun conn(id: Identifier) = queryStateWithProof<IbcConnection>(id).first
    fun connProof(id: Identifier) = queryStateWithProof<IbcConnection>(id).second

    fun chan(id: Identifier) = queryStateWithProof<IbcChannel>(id).first
    fun chanProof(id: Identifier) = queryStateWithProof<IbcChannel>(id).second

    private fun <T> executeFlow(logic: net.corda.core.flows.FlowLogic<T>) : T {
        val future = mockNode.startFlow(logic)
        mockNet.runNetwork()
        return future.get()
    }

    fun createGenesis(participants: List<Party>) {
        val stx = executeFlow(IbcGenesisCreateFlow(
                participants
        ))
        setBaseId(StateRef(stx.tx.id, 0))
    }

    fun createHost() {
        val stx = executeFlow(IbcHostCreateFlow(baseId))
        val host = stx.tx.outputsOfType<Host>().single()
        assert(host.baseId == baseId)
    }

    fun createBank() {
        val stx = executeFlow(IbcBankCreateFlow(baseId))
        val bank = stx.tx.outputsOfType<Bank>().single()
        assert(bank.baseId == baseId)
    }

    fun createCashBank(bank: Party) {
        val stx = executeFlow(IbcCashBankCreateFlow(baseId, bank))
        val cashBank = stx.tx.outputsOfType<CashBank>().single()
        assert(cashBank.baseId == baseId)
        assert(cashBank.owner == bank)
    }

    fun allocateFund(owner: Address, denom: Denom, amount: Amount) {
        val orgAmount = bank().allocated[denom]?.get(owner) ?: Amount.ZERO
        val stx = executeFlow(IbcFundAllocateFlow(
                baseId,
                owner,
                denom,
                amount
        ))
        val bank = stx.tx.outputsOfType<Bank>().single()
        assert(bank.allocated[denom]!![owner]!! == orgAmount + amount)
    }

    fun allocateCash(
            recipient: Party,
            amount: Long,
            currency: Currency
    ) {
        val result = executeFlow(CashIssueAndPaymentFlow(
                amount = AMOUNT(amount, currency),
                issueRef = OpaqueBytes(ByteArray(1)),
                recipient = recipient,
                notary = host().notary,
                anonymous = false
        ))
        val cash = result.stx.tx.outputsOfType<Cash.State>().single()
        assert(cash.amount.quantity == amount)
        assert(cash.amount.token.issuer.party == mockNode.info.legalIdentities.single())
        assert(cash.amount.token.product == currency)
    }

    fun createClient(msg: MsgCreateClient) : Identifier {
        val stx = executeFlow(IbcClientCreateFlow(baseId, msg))
        val client = stx.tx.outputsOfType<ClientState>().single()
        return client.id
    }

    fun connOpenInit(msg: MsgConnectionOpenInit) : Identifier {
        val stx = executeFlow(IbcConnOpenInitFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.end.state == Connection.State.STATE_INIT)
        return conn.id
    }

    fun connOpenTry(msg: MsgConnectionOpenTry) : Identifier {
        val stx = executeFlow(IbcConnOpenTryFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.end.state == Connection.State.STATE_TRYOPEN)
        return conn.id
    }

    fun connOpenAck(msg: MsgConnectionOpenAck) {
        val stx = executeFlow(IbcConnOpenAckFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.end.state == Connection.State.STATE_OPEN)
    }

    fun connOpenConfirm(msg: MsgConnectionOpenConfirm) {
        val stx = executeFlow(IbcConnOpenConfirmFlow(baseId, msg))
        val conn = stx.tx.outputsOfType<IbcConnection>().single()
        assert(conn.end.state == Connection.State.STATE_OPEN)
    }

    fun chanOpenInit(msg: MsgChannelOpenInit) : Identifier {
        val stx = executeFlow(IbcChanOpenInitFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.end.state == ChannelOuterClass.State.STATE_INIT)
        return chan.id
    }

    fun chanOpenTry(msg: MsgChannelOpenTry) : Identifier {
        val stx = executeFlow(IbcChanOpenTryFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.end.state == ChannelOuterClass.State.STATE_TRYOPEN)
        return chan.id
    }

    fun chanOpenAck(msg: MsgChannelOpenAck) {
        val stx = executeFlow(IbcChanOpenAckFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.end.state == ChannelOuterClass.State.STATE_OPEN)
    }

    fun chanOpenConfirm(msg: MsgChannelOpenConfirm) {
        val stx = executeFlow(IbcChanOpenConfirmFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.end.state == ChannelOuterClass.State.STATE_OPEN)
    }

    fun chanCloseInit(msg: MsgChannelCloseInit) {
        val stx = executeFlow(IbcChanCloseInitFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.end.state == ChannelOuterClass.State.STATE_CLOSED)
    }

    fun chanCloseConfirm(msg: MsgChannelCloseConfirm) {
        val stx = executeFlow(IbcChanCloseConfirmFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.end.state == ChannelOuterClass.State.STATE_CLOSED)
    }

    fun sendPacket(packet: ChannelOuterClass.Packet) {
        val stx = executeFlow(IbcSendPacketFlow(baseId, packet))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceSend == packet.sequence + 1)
        assert(chan.packets[packet.sequence] == packet)
    }

    fun recvPacketOrdered(msg: MsgRecvPacket) {
        val stx = executeFlow(IbcRecvPacketFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceRecv == msg.packet.sequence + 1)
    }

    fun recvPacketUnordered(msg: MsgRecvPacket) {
        val stx = executeFlow(IbcRecvPacketFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceRecv == 1L)
        assert(chan.receipts.contains(msg.packet.sequence))
    }

    fun acknowledgePacketOrdered(msg: MsgAcknowledgement) {
        val stx = executeFlow(IbcAcknowledgePacketFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceAck == msg.packet.sequence + 1)
        assert(!chan.packets.contains(msg.packet.sequence))
    }

    fun acknowledgePacketUnordered(msg: MsgAcknowledgement) {
        val stx = executeFlow(IbcAcknowledgePacketFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        assert(chan.nextSequenceAck == 1L)
        assert(!chan.packets.contains(msg.packet.sequence))
    }

    fun sendTransfer(msg: MsgTransfer) {
        val prevBank = bank()
        val stx = executeFlow(IbcSendTransferFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        val packet = chan.packets[chan.nextSequenceSend - 1]!!
        assert(packet.sequence == chan.nextSequenceSend - 1)
        assert(packet.sourcePort == msg.sourcePort)
        assert(packet.sourceChannel == msg.sourceChannel)
        assert(packet.destinationPort == chan.end.counterparty.portId)
        assert(packet.destinationChannel == chan.end.counterparty.channelId)
        assert(packet.timeoutHeight == msg.timeoutHeight)
        assert(packet.timeoutTimestamp == msg.timeoutTimestamp)
        val data = packet.data.toFungibleTokenPacketData()
        assert(data.denom == msg.token.denom)
        assert(data.amount == msg.token.amount.toLong())
        assert(data.sender == msg.sender)
        assert(data.receiver == msg.receiver)
        val bank = stx.tx.outputsOfType<Bank>().single()
        val denom = Denom.fromString(data.denom)
        val amount = Amount.fromLong(data.amount)
        val sender = Address.fromBech32(msg.sender)
        if (denom.hasPrefix(Identifier(msg.sourcePort), Identifier(msg.sourceChannel))) {
            assert(prevBank.burn(sender, denom, amount) == bank)
        } else {
            assert(prevBank.lock(sender, denom, amount) == bank)
        }
    }

    fun transfer(msg: MsgTransfer) {
        val stx = executeFlow(IbcTransferFlow(baseId, msg))
        val chan = stx.tx.outputsOfType<IbcChannel>().single()
        val packet = chan.packets[chan.nextSequenceSend - 1]!!
        assert(packet.sequence == chan.nextSequenceSend - 1)
        assert(packet.sourcePort == msg.sourcePort)
        assert(packet.sourceChannel == msg.sourceChannel)
        assert(packet.destinationPort == chan.end.counterparty.portId)
        assert(packet.destinationChannel == chan.end.counterparty.channelId)
        assert(packet.timeoutHeight == msg.timeoutHeight)
        assert(packet.timeoutTimestamp == msg.timeoutTimestamp)
        val data = packet.data.toFungibleTokenPacketData()
        val denom =
                if (msg.token.denom.hasPrefixes("ibc"))
                    cashBank().resolveDenom(msg.token.denom)
                else
                    Denom.fromString(msg.token.denom)
        assert(data.denom == denom.toString())
        assert(data.amount == msg.token.amount.toLong())
        assert(data.sender == msg.sender)
        assert(data.receiver == msg.receiver)
    }
}
