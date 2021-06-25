IBC/TAO for Corda
=================

IBC Overview
------------

The inter-blockchain communication protocol(IBC_) is a protocol that
allows heterogeneous blockchains to connect to each other without
trusting a middleman (called the relayer).
This protocol consists of the following two layers.

.. _IBC: https://github.com/cosmos/ibc

Transport, Authentication and Ordering layer protocol (IBC/TAO)
  The protocol for communication (handshaking and packet
  transmission / reception) between blockchains

Application layer protocol (IBC/APP)
  The general term for protocols, linking blockchain
  applications (smart contracts) that work on two blockchains,
  based on IBC/TAO

Packets are sent, received and acknowledged by IBC/TAO as follows.

#. Source chain sends packet
#. Dest chain receives packet and replys ACK

   -  Dest chain verifies that source chain sent packet correctly

#. Source chain receives ACK

   -  Source chain verifies that dest chain received packet correctly

On the other hand, cross-chain applications (IBC/APP) work as follows.

#. Source chain sends packet and **performs additional processing
   defined for send by an IBC/APP protocol**
#. Dest chain receives packet, **performs additional processing defined
   for receive by an IBC/APP protocol**, and replys ACK

   -  Dest chain verifies that source chain sent packet correctly

#. Source chain receives ACK and **performs additional processing
   defined for ACK-receive by an IBC/APP protocol**

   -  Source chain verifies that dest chain received packet correctly

Comparing the above two, you can find that any IBC/APP protocols use
the same mechanism defined by IBC/TAO for blockchain interoperability.
The feature of IBC is that a wide variety of cross-chain applications
can be easily designed by simply defining plugins to be added to packet
transmission, reception and acknowledgement through IBC/TAO.

Requirements for blockchains that can support IBC
-------------------------------------------------

The detailed requirements for blockchains to be connected over IBC
are specified in `ICS-24`_,
but they are roughly as follows.

.. _`ICS-24`: https://github.com/cosmos/ibc/tree/master/spec/core/ics-024-host-requirements

-  State data specified by IBC (for example, ClientState,
   ConsensusState, Connection, Channel, PacketCommitment,
   Acknowledgement) can be stored in an unmodifiable manner.
-  State data specified by IBC must be able to transition in
   accordance with the rules specified by IBC.
-  State data specified by IBC cannot be modified or erased in a
   manner other than the rules specified by IBC
-  State data specified by IBC can be uniquely identified and can be
   queried.
-  State data acquired through the RPC can be verified to be valid,
   outside the blockchain, using an appropriate light client
   protocol.

Approach of Corda-IBC
---------------------

-  Corda-IBC is a framework allowing for communication through IBC
   between Corda networks or between a Corda network and other
   blockchain.
-  However, Corda network or ledger is not itself a platform that meets
   the requirements of IBC.
-  Therefore, Corda-IBC does not treat an entire Corda network or ledger
   as a endpoint of communication through IBC; instead, it constructs
   IBC-compliant states and transitions inside an application running on
   Corda, called a CorDapp, and connects them to other blockchains using
   IBC.

Problem: How can we uniquely identify state data in Corda?
----------------------------------------------------------

-  IBC requires that states can be uniquely identified (and queried).
-  On the other hand, state data in Corda is difficult to uniquely
   identify.
-  Of course, it is easy to make a state have an immutable ID.

   -  For example, it is sufficient to assign an ID at the time of state
      generation and to enforce by contract that the ID does not change
      during subsequent state transitions.

-  But Corda doesn't have a mechanism to ensure consistency across all
   states on the network, so it's difficult to prohibit states from having
   duplicate IDs, even if they belong to the same contract.

Approach: Genesis-Host method
-----------------------------

-  Overview

   -  Somebody (anyone is ok) generates a state called Genesis first,
      then consumes it and generates a state called Host instead.
   -  At this time, the txid of the transaction including Genesis is
      embedded as prefix of the ID of the Host.
   -  Since a notary ensures that Genesis can be consumed by only one
      transaction, this also ensures the uniqueness of the ID of the
      Host.
   -  When generating various IBC-complicant states, by enforcing them
      to have the same ID prefix as that of the Host, we can enforce the
      uniqueness of the IDs of these states.
   -  This Host plays as an endpoint to be connected to other blockchain
      using the IBC protocol.

-  Genesis state

   -  A state used to make an ID of Host unique
   -  Txids of transactions containing Genesis is set to Host as
      "baseID".
   -  Under a single notary, all transactions have different txids and a
      single transaction can't be consumed more than once, ensuring that
      only one Host can have a given baseID.

-  Host state

   -  A state that centrally manages the issuance of IDs for various
      IBC-compliant states
   -  Host generates all IBC states so that their IDs are unique to each
      other and they all have "baseID" same as that of the Host.
   -  States belonging to different Hosts can be distinguished because
      they have different baseIDs.

-  Uniqueness of ID in Genesis-Host method

   -  Notary can be uniquely identified by public key
   -  Within the scope of notary, Host can be uniquely identified by
      baseID
   -  Within the scope of Host, each state can be uniquely identified by
      stateID
   -  Therefore, the combination of notary public key + baseID + stateID
      uniquely identifies every state of IBC

Light client for Corda-IBC
--------------------------

-  Light client consists of the following data

   -  The public key of a notary

      -  This key uniquely identifies the notary under which the Host
         that is the endpoint of IBC exists.

   -  baseID

      -  This baseID uniquely identifies the Host that is the endpoint
         of IBC.

         -  The Host is regarded as a "blockchain" for the Light client.

-  How to verify state

   -  Proof

      -  The transaction signed by the notary and containing the output to
         be verified is used as proof.

   -  Verification for state existence works as follows.

      #.  Checking that the transaction is signed by the given notary.
      #.  Checking that the state is included in output of the transaction.
      #.  Checking that the state has the given baseID.

Limitation: Corda-IBC can't handle packet receive timeout
---------------------------------------------------------

-  In the IBC spec, a packet expiration date in units of height or
   timestamp can be set when a packet is transmitted. If a packet isn't
   received by the expiration date, receiving the packet will fail.
-  Corda-IBC does not define height or timestamp, so if Corda is the
   receiver of the packet, it cannot handle the receive timeout.

Detailed specification of IBC/TAO for Corda
-------------------------------------------

IBC contract: Host part
~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: Genesis
   :noindex:

   A state class determining baseID of Host

   .. attribute:: participants
      :type: List<AbstractParty>

      A set of users to share Genesis, Host and all states to be
      generated by the Host

.. class:: Host
   :noindex:

   A state class representing an endpoint of communication via Corda-IBC

   .. attribute:: participants
      :type: List<AbstractParty>

      participants inherited from Genesis (with the same content)

   .. attribute:: baseId
      :type: StateRef

      Reference to Genesis (txhash)

   .. attribute:: notary
      :type: Party

      Identity of notary that notarized Genesis

   .. attribute:: clientIds
      :type: List<Identifier>

      A list of IDs of ClientState states created under Host, used to
      prevent duplicate IDs from being assigned to different ClientState
      states

   .. attribute:: connIds
      :type: List<Identifier>

      A list of IDs of Connection states created under Host, used to
      prevent duplicate IDs from being assigned to different Connection
      states.

   .. attribute:: portChanIds
      :type: List<Pair<Identifier, Identifier>>

      A list of pairs of portId and channelID of Channel states created
      under Host, used to prevent duplicate ID pairs from being
      assigned, but allowing identical channelIDs to be used on
      different ports.

Commands
^^^^^^^^

.. class:: GenesisCreate
   :noindex:

   A command creating Genesis

   .. attribute:: Input states

      -  None

   .. attribute:: Output states

      -  Genesis

   .. attribute:: Contract rules

      -  Transaction must not contain any other input or output state

.. class:: HostCreate
   :noindex:

   A command consuming Genesis and creating Host

   .. attribute:: Input states

      -  Genesis

   .. attribute:: Output states

      -  Host

   .. attribute:: Contract rules

      -  Host.participants == Genesis.participants
      -  Host.baseId == refOf(Genesis)
      -  Host.notary == notaryOf(Genesis)

IBC contract: Client part
~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: ClientState
   :noindex:

   A state class representing ClientState defined in ICS-2

   .. attribute:: participants
      :type: List<AbstractParty>

      participants inherited from Host

   .. attribute:: baseId
      :type: StateRef

      baseId inherited from Host

   .. attribute:: id
      :type: Identifier

      Identifier of the ClientState

   .. attribute:: clientState
      :type: Any

      ClientState body encoded in protobuf, of which content is
      different for each type of blockchain that ClientState verifies

   .. attribute:: consensusStates
      :type: Map<Height, ConsensusState>

      Associative array with height as the key and ConsensusState as the
      value

Commands
^^^^^^^^

.. class:: HandleClientCreate(msg: MsgCreateClient)
   :noindex:

   A command creating a new ClientState state

   .. attribute:: Input states

      -  Host

   .. attribute:: Output states

      -  Host (clientIds + = <ID of ClientState to be created>)
      -  ClientState

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All input states must also be included in output states
         (because states of Corda-IBC are never deleted once created)
      -  ID of the new ClientState must be added to clientIds of Host.
      -  ClientState must be correctly created using ClientState and
         ConsensusState given in MsgCreateClient and set in transaction
         output.

IBC contract: Connection part
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: IbcConnection
   :noindex:

   A state class representing Connection defined in ICS-3

   .. attribute:: participants
      :type: List<AbstractParty>

      participants inherited from Host (with the same content)

   .. attribute:: baseId
      :type: StateRef

      baseId inherited from Host (with the same value)

   .. attribute:: id
      :type: Identifier

      Identifier of this connection

   .. attribute:: end
      :type: ConnectionEnd

      Instance of the ConnectionEnd class automatically generated by
      protobuf

Commands
^^^^^^^^

.. class:: HandleConnOpenInit(msg: MsgConnectionOpenInit)
   :noindex:

   A command executing connOpenInit

   .. attribute:: Input states

      -  Host
      -  [readonly] ClientState

   .. attribute:: Output states

      -  Host (connIds + = <ID of the new connection>)
      -  IbcConnection(end.state == INIT)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  ID of the new IbcConnection must be added to connIds of the
         Host.
      -  Requirements for connOpenInit defined in ICS-3 must be filled.

.. class:: HandleConnOpenTry(msg: MsgConnectionOpenTry)
   :noindex:

   A command executing connOpenTry

   .. attribute:: Input states

      -  Host
      -  [readonly] ClientState
      -  [optional] IbcConnection(end.state == INIT)

         -  As specified in ICS-3, the IbcConnection in INIT state may
            or may not have been created in advance.

   .. attribute:: Output states

      -  Host (connIds + = <ID of the new IbcConnection ID>)
      -  IbcConnection(end.state == TRYOPEN)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  ID of the new IbcConnection must be added to connIds of Host.

         -  If IbcConnection has been created in advance and set in
            input, new IbcConnection must not created.

      -  Requirements for connOpenTry defined in ICS-3 must be filled.

.. class:: HandleConnOpenAck(msg: MsgConnectionOpenAck)
   :noindex:

   A command executing connOpenAck

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  IbcConnection(end.state == INIT or TRYOPEN)

         -  As specified in ICS-3, there are two possible IbcConnection
            states: INIT or TRYOPEN.

   .. attribute:: Output states

      -  IbcConnection(end.state == OPEN)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Requirements for connOpenAck defined in ICS-3 must be filled.

.. class:: HandleConnOpenConfirm(msg: MsgConnectionOpenConfirm)
   :noindex:

   A command executing connOpenConfirm

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  IbcConnection(end.state == TRYOPEN)

   .. attribute:: Output states

      -  IbcConnection(end.state == OPEN)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Requirements for connOpenConfirm defined in ICS-3 must be
         filled.

IBC contract: Channel part
~~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: IbcChannel
   :noindex:

   A state class representing Channel define in ICS-4

   .. attribute:: participants
      :type: List<AbstractParty>

      participants inherited from Host (with the same content)

   .. attribute:: baseId
      :type: StateRef

      baseId inherited from Host (with the same value)

   .. attribute:: id
      :type: Identifier

      Identifier of this channel

   .. attribute:: portId
      :type: Identifier

      Port identifier of this channel

   .. attribute:: end
      :type: Channel

      Instance of Channel class automatically generated by protobuf

   .. attribute:: nextSequenceSend
      :type: Long

      Sequence to be used for next packet to be sent

   .. attribute:: nextSequenceRecv
      :type: Long

      Sequence of next packet to be received (used only for channel with
      type of ORDERED)

   .. attribute:: nextSequenceAck
      :type: Long

      Sequence of next ACK to be received (used only for channel with
      type of ORDERED)

   .. attribute:: packets
      :type: Map<Long, Packet>

      An associative array with sequence as the key and packet as the
      value

   .. attribute:: receipts
      :type: Set<Long>

      A set of sequences of received packets

   .. attribute:: acknowledgements
      :type: Map<Long, Acknowledgement>

      An associative array with sequence as the key and acknowledgement
      as the value

Commands
^^^^^^^^

.. class:: HandleChanOpenInit(msg: MsgChannelOpenInit)
   :noindex:

   A command executing chanOpenInit

   .. attribute:: Input states

      -  Host
      -  [readonly] IbcConnection

   .. attribute:: Output states

      -  Host(portChanIds += Pair(Channel.portId, Channel.id))
      -  IbcChannel(end.state = INIT)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Port Id and Channel ID of the new IbcChannel must be added to
         portChanIds of Host
      -  Requirements for chanOpenInit defined in ICS-4 must be
         satisfied.

.. class:: HandleChanOpenTry(msg: MsgChannelOpenTry)
   :noindex:

   A command executing chanOpenTry

   .. attribute:: Input states

      -  Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  [optional] IbcChannel(end.state = INIT)

         -  As specified in ICS-4, IbcChannel in INIT state may or may
            not have been created in advance.

   .. attribute:: Output states

      -  Host(portChanIds += Pair(Channel.portId, Channel.id))
      -  IbcChannel(end.state = TRYOPEN)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Port ID and channel ID of the new IbcChannel must be added to
         the portChanIds of Host

         -  If the IbcChannel has been created in advance, new
            IbcChannel must not be created.

      -  Requirements for chanOpenTry defined in ICS-4 must be
         satisfied.

.. class:: HandleChanOpenAck(msg: MsgChannelOpenAck)
   :noindex:

   A command executing chanOpenAck

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel(end.state = INIT or TRYOPEN)

         -  As specified in ICS-4, there are two possible IbcChannel
            states : INIT or TRYOPEN.

   .. attribute:: Output states

      -  IbcChannel(end.state = OPEN)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Requirements for chanOpenAck defined in ICS-4 must be
         satisfied.

.. class:: HandleChanOpenConfirm(msg: MsgChannelOpenConfirm)
   :noindex:

   A command executing chanOpenConfirm

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel(end.state = TRYOPEN)

   .. attribute:: Output states

      -  IbcChannel(end.state = OPEN)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Requirements for chanOpenConfirm defined in ICS-4 must be
         satisfied.

.. class:: HandleChanCloseInit(msg: MsgChannelCloseInit)
   :noindex:

   A command executing chanCloseInit

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] IbcConnection
      -  IbcChannel(end.state = OPEN)

   .. attribute:: Output states

      -  IbcChannel(end.state = CLOSED)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Requirements for chanCloseInit defined in ICS-4 must be
         satisfield.

.. class:: HandleChanCloseConfirm(msg: MsgChannelCloseConfirm)
   :noindex:

   A command executing chanCloseConfirm

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel(end.state = OPEN)

   .. attribute:: Output states

      -  IbcChannel(end.state = CLOSED)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All Input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created)
      -  Requirements for chanCloseConfirm defined in ICS-4 must be
         satisfied.
