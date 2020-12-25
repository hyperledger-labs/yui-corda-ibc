package jp.datachain.corda.ibc.states

import ibc.core.channel.v1.ChannelOuterClass
import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class IbcChannel private constructor (
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        override val id: Identifier,
        val portId: Identifier,
        val end: ChannelOuterClass.Channel,
        val nextSequenceSend: Long,
        val nextSequenceRecv: Long,
        val nextSequenceAck: Long,
        val packets: Map<Long, ChannelOuterClass.Packet>,
        val receipts: Set<Long>,
        val acknowledgements: Map<Long, ChannelOuterClass.Acknowledgement>
) : IbcState {
    constructor(host: Host, portId: Identifier, chanId: Identifier, end: ChannelOuterClass.Channel) : this(
            host.participants,
            host.baseId,
            chanId,
            portId,
            end,
            1,
            1,
            1,
            emptyMap(),
            emptySet(),
            emptyMap())
}
