package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.contracts.Ibc
import jp.datachain.corda.ibc.ics24.Host
import jp.datachain.corda.ibc.ics24.Identifier
import jp.datachain.corda.ibc.ics4.Acknowledgement
import jp.datachain.corda.ibc.ics4.ChannelEnd
import jp.datachain.corda.ibc.ics4.Packet
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.StateRef
import net.corda.core.identity.AbstractParty

@BelongsToContract(Ibc::class)
data class IbcChannel private constructor (
        override val participants: List<AbstractParty>,
        override val baseId: StateRef,
        override val id: Identifier,
        val portId: Identifier,
        val end: ChannelEnd,
        val nextSequenceSend: Long,
        val nextSequenceRecv: Long,
        val nextSequenceAck: Long,
        val packets: Map<Long, Packet>,
        val acknowledgements: Map<Long, Acknowledgement>
) : IbcState {
    constructor(host: Host, portId: Identifier, chanId: Identifier, end: ChannelEnd) : this(
            host.participants,
            host.baseId,
            chanId,
            portId,
            end,
            1,
            1,
            1,
            emptyMap(),
            emptyMap())
}
