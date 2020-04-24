package jp.datachain.corda.ibc.ics23

import jp.datachain.corda.ibc.ics24.Identifier

/*
Store 			Path format 															Value type 			Defined in
provableStore 	"clients/{identifier}/type" 											ClientType 			ICS 2
privateStore 	"clients/{identifier}" 													ClientState 		ICS 2
provableStore 	"clients/{identifier}/consensusStates/{height}" 						ConsensusState 		ICS 7
privateStore 	"clients/{identifier}/connections 										[]Identifier 		ICS 3
provableStore 	"connections/{identifier}" 												ConnectionEnd 		ICS 3
privateStore 	"ports/{identifier}" 													CapabilityKey 		ICS 5
provableStore 	"ports/{identifier}/channels/{identifier}" 								ChannelEnd 			ICS 4
provableStore 	"ports/{identifier}/channels/{identifier}/key" 							CapabilityKey 		ICS 4
provableStore 	"ports/{identifier}/channels/{identifier}/nextSequenceRecv" 			uint64 				ICS 4
provableStore 	"ports/{identifier}/channels/{identifier}/packets/{sequence}" 			bytes 				ICS 4
provableStore 	"ports/{identifier}/channels/{identifier}/acknowledgements/{sequence}" 	bytes 				ICS 4
privateStore 	"callbacks/{identifier}" 												ModuleCallbacks 	ICS 26
*/
sealed class Value {
    data class ClientType(val clientType: jp.datachain.corda.ibc.ics2.ClientType) : Value()
    data class ClientState(val clientState: jp.datachain.corda.ibc.ics2.ClientState) : Value()
    data class ConsensusState(val consensusState: jp.datachain.corda.ibc.ics2.ConsensusState) : Value()
    data class Identifiers(val identifiers: Array<Identifier>) : Value()
    data class ConnectionEnd(val connectionEnd: jp.datachain.corda.ibc.ics3.ConnectionEnd) : Value()
    data class CapabilityKey(val capabilityKey: jp.datachain.corda.ibc.ics5.CapabilityKey) : Value()
    data class ChannelEnd(val channelEnd: jp.datachain.corda.ibc.ics4.ChannelEnd) : Value()
    data class Sequence(val sequence: Int) : Value()
    data class Packet(val packet: jp.datachain.corda.ibc.ics4.Packet) : Value()
    data class Acknowledgement(val acknowledgement: jp.datachain.corda.ibc.ics4.Acknowledgement) : Value()
    //data class ModuleCallbacks(val moduleCallbacks: jp.datachain.corda.ibc.ics26.ModuleCallbacks) : Value()
}
