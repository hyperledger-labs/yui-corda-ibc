package jp.datachain.corda.ibc.relayer

import jp.datachain.corda.ibc.clients.corda.CordaCommitmentProof
import jp.datachain.corda.ibc.clients.corda.CordaConsensusState
import jp.datachain.corda.ibc.flows.*
import jp.datachain.corda.ibc.ics2.ClientState
import jp.datachain.corda.ibc.ics2.ClientType
import jp.datachain.corda.ibc.ics23.CommitmentPrefix
import jp.datachain.corda.ibc.ics23.CommitmentProof
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelOrder
import jp.datachain.corda.ibc.ics4.Packet
import jp.datachain.corda.ibc.states.Connection
import jp.datachain.corda.ibc.types.Height
import jp.datachain.corda.ibc.types.Version
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort

object Relayer {
    @JvmStatic
    fun main(args: Array<String>) {
        val hostAndPort = NetworkHostAndPort("localhost", 10006)
        val client = CordaRPCClient(hostAndPort)
        client.start("user1", "test").use {
            val ops = it.proxy

            val participants = listOf("PartyA", "PartyB")
                    .map{ops.partiesFromName(it, false).single()}

            val hostA = createHost(ops, participants).tx.outputsOfType<Host>().single()
            val hostB = createHost(ops, participants).tx.outputsOfType<Host>().single()

            val cordaConsensusStateA = hostA.getConsensusState(Height(0)) as CordaConsensusState
            val cordaConsensusStateB = hostB.getConsensusState(Height(0)) as CordaConsensusState

            val clientA = createClient(ops, hostA.id, cordaConsensusStateB).tx.outputsOfType<ClientState>().single()
            val clientB = createClient(ops, hostB.id, cordaConsensusStateA).tx.outputsOfType<ClientState>().single()

            val connIdB = clientB.generateIdentifier()
            val stxInit = connOpenInit(ops,
                    hostA.id,
                    connIdB,
                    hostB.getCommitmentPrefix(),
                    clientA.id,
                    clientB.id)
            val connInit = stxInit.tx.outputsOfType<Connection>().single()
            println("connInit: ${connInit}")

            val stxTry = connOpenTry(ops,
                    hostB.id,
                    connIdB,
                    connInit.id,
                    hostA.getCommitmentPrefix(),
                    clientA.id,
                    clientB.id,
                    hostA.getCompatibleVersions(),
                    CordaCommitmentProof(stxInit.coreTransaction, stxInit.sigs.filter{it.by == hostA.notary.owningKey}.single()),
                    CordaCommitmentProof(stxInit.coreTransaction, stxInit.sigs.filter{it.by == hostA.notary.owningKey}.single()),
                    hostA.getCurrentHeight(),
                    hostB.getCurrentHeight())
            val connTry = stxTry.tx.outputsOfType<Connection>().single()
            println("connTry: ${connTry}")

            val stxAck = connOpenAck(ops,
                    hostA.id,
                    connInit.id,
                    connTry.end.version as Version.Single,
                    CordaCommitmentProof(stxTry.coreTransaction, stxTry.sigs.filter{it.by == hostB.notary.owningKey}.single()),
                    CordaCommitmentProof(stxTry.coreTransaction, stxTry.sigs.filter{it.by == hostB.notary.owningKey}.single()),
                    hostB.getCurrentHeight(),
                    hostA.getCurrentHeight())
            val connAck = stxAck.tx.outputsOfType<Connection>().single()
            println("connAck: ${connAck}")

            val stxConfirm = connOpenConfirm(ops,
                    hostB.id,
                    connTry.id,
                    CordaCommitmentProof(stxAck.coreTransaction, stxAck.sigs.filter{it.by == hostA.notary.owningKey}.single()),
                    hostA.getCurrentHeight())
            val connConfirm = stxConfirm.tx.outputsOfType<Connection>().single()
            println("connConfirm: ${connConfirm}")
        }
    }

    fun createHost(ops: CordaRPCOps, participants: List<Party>) : SignedTransaction {
        ops.startFlowDynamic(
                IbcHostSeedCreateFlow.Initiator::class.java,
                participants
        ).returnValue.get()
        return ops.startFlowDynamic(IbcHostCreateFlow.Initiator::class.java).returnValue.get()
    }

    fun createClient(ops: CordaRPCOps, hostId: Identifier, cordaConsensusState: CordaConsensusState) = ops.startFlowDynamic(
            IbcClientCreateFlow.Initiator::class.java,
            hostId,
            ClientType.CordaClient,
            cordaConsensusState ).returnValue.get()

    fun connOpenInit(
            ops: CordaRPCOps,
            hostId: Identifier,
            desiredConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            clientIdentifier: Identifier,
            counterpartyClientIdentifier: Identifier
    ) = ops.startFlowDynamic(
            IbcConnOpenInitFlow.Initiator::class.java,
            hostId,
            desiredConnectionIdentifier,
            counterpartyPrefix,
            clientIdentifier,
            counterpartyClientIdentifier).returnValue.get()

    fun connOpenTry(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            desiredIdentifier: Identifier,
            counterpartyConnectionIdentifier: Identifier,
            counterpartyPrefix: CommitmentPrefix,
            counterpartyClientIdentifier: Identifier,
            clientIdentifier: Identifier,
            counterpartyVersions: Version.Multiple,
            proofInit: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) = ops.startFlowDynamic(
            IbcConnOpenTryFlow.Initiator::class.java,
            hostIdentifier,
            desiredIdentifier,
            counterpartyConnectionIdentifier,
            counterpartyPrefix,
            counterpartyClientIdentifier,
            clientIdentifier,
            counterpartyVersions,
            proofInit,
            proofConsensus,
            proofHeight,
            consensusHeight).returnValue.get()

    fun connOpenAck(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            identifier: Identifier,
            version: Version.Single,
            proofTry: CommitmentProof,
            proofConsensus: CommitmentProof,
            proofHeight: Height,
            consensusHeight: Height
    ) = ops.startFlowDynamic(
            IbcConnOpenAckFlow.Initiator::class.java,
            hostIdentifier,
            identifier,
            version,
            proofTry,
            proofConsensus,
            proofHeight,
            consensusHeight).returnValue.get()

    fun connOpenConfirm(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            identifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) = ops.startFlowDynamic(
            IbcConnOpenConfirmFlow.Initiator::class.java,
            hostIdentifier,
            identifier,
            proofAck,
            proofHeight).returnValue.get()

    fun chanOpenInit(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            order: ChannelOrder,
            connectionHops: Array<Identifier>,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version.Single
    ) = ops.startFlowDynamic(
            IbcChanOpenInitFlow.Initiator::class.java,
            hostIdentifier,
            order,
            connectionHops,
            counterpartyPortIdentifier,
            counterpartyChannelIdentifier,
            version).returnValue.get()

    fun chanOpenTry(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            order: ChannelOrder,
            connectionHops: Array<Identifier>,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyPortIdentifier: Identifier,
            counterpartyChannelIdentifier: Identifier,
            version: Version.Single,
            counterpartyVersion: Version.Single,
            proofInit: CommitmentProof,
            proofHeight: Height
    ) = ops.startFlowDynamic(
            IbcChanOpenTryFlow.Initiator::class.java,
            hostIdentifier,
            order,
            connectionHops,
            portIdentifier,
            channelIdentifier,
            counterpartyPortIdentifier,
            counterpartyChannelIdentifier,
            version,
            counterpartyVersion,
            proofInit,
            proofHeight).returnValue.get()

    fun chanOpenAck(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            counterpartyVersion: Version.Single,
            proofTry: CommitmentProof,
            proofHeight: Height
    ) = ops.startFlowDynamic(
            IbcChanOpenAckFlow.Initiator::class.java,
            hostIdentifier,
            portIdentifier,
            channelIdentifier,
            counterpartyVersion,
            proofTry,
            proofHeight).returnValue.get()

    fun chanOpenConfirm(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            portIdentifier: Identifier,
            channelIdentifier: Identifier,
            proofAck: CommitmentProof,
            proofHeight: Height
    ) = ops.startFlowDynamic(
            IbcChanOpenConfirmFlow.Initiator::class.java,
            hostIdentifier,
            portIdentifier,
            channelIdentifier,
            proofAck,
            proofHeight).returnValue.get()

    fun sendPacket(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            packet: Packet
    ) = ops.startFlowDynamic(
            IbcSendPacketFlow.Initiator::class.java,
            hostIdentifier,
            packet).returnValue.get()

    fun recvPacket(
            ops: CordaRPCOps,
            hostIdentifier: Identifier,
            packet: Packet,
            proof: CommitmentProof,
            proofHeight: Height,
            acknowledgement: Acknowledgement
    ) = ops.startFlowDynamic(
            IbcRecvPacketFlow.Initiator::class.java,
            hostIdentifier,
            packet,
            proof,
            proofHeight,
            acknowledgement).returnValue.get()
}
