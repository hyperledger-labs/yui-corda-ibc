ICS-20 for Corda
================

ICS-20 Overview
---------------

`ICS-20 <https://github.com/cosmos/ibc/blob/master/spec/app/ics-020-fungible-token-transfer/README.md>`_
is an IBC/APP protocol included in the IBC spec
that enables a two-way peg between blockchains by linking packet
transmission, reception and acknowledgement with
token locking, unlocking, burning and minting.
This cross-chain protocol works as follows.

1. Src: The sender of a token sends a packet to a destination chain
   and at the same time locks a token in escrow.
2. Dst: The IBC module on the destination chain receives the packet
   and at the same time mints a voucher token to a receiver.
3. Dst: The minted voucher token can be transfered arbitrarily.
4. Dst: The sender of a voucher token sends a packet to the source
   chain and at the same time burns the voucher token.
5. Src: The IBC module on the source chain receives the packet
   and at the same time unlocks a token in escrow.

Design of ICS-20 for Corda (when using the Cash contract of Corda)
------------------------------------------------------------------

Key point
~~~~~~~~~

-  ICS-20 assumes that smart contract called the IBC module is
   responsible for escrowing tokens and minting vouchers.
-  But Corda's existing asset (e.g. Cash) is supposed to be owned by a
   specific user (node), making it difficult for tokens to be locked in
   smart contracts or to be minted by smart contracts.
-  Therefore in the current implementation of Corda-IBC, a trusted
   special node (such as a financial institution) is responsible for the
   role (escrowing tokens and minting vouchers).

   -  Hereinafter, this node will be referred to as "bank".

Security assumption
~~~~~~~~~~~~~~~~~~~

-  It is assumed that an issuer of Cash issues Cash tokens properly in a
   source chain. (trust in the issuer)
-  It is assumed that bank nodes lock (execute Cash.Move), unlock
   (execute Cash.Move), mint (execute Cash.Issue) and burn (execute
   Cash.Exit) tokens only through ICS-20, in both chains. (trust in the
   bank nodes)

Procedure
~~~~~~~~~

1. Src: The sender of Cash sends a packet over IBC/TAO and at the same
   time executes "Cash.Move" to transfer his Cash state to the bank user
   on the source chain.
2. Dst: The bank user on the destination chain receives the packet over
   IBC/TAO and at the same time executes "Cash.Issue" and "Cash.Move" to
   mint a new Cash state to a recipient.
3. Dst: The minted Cash state can be transfered arbitrarily on the
   destination chain.
4. Dst: The sender of a Cash state on the destination chain sends a packet over
   IBC/TAO and at the same time executes "Cash.Move" and "Cash.Exit" to
   burn his Cash state.
5. Src: The bank user on the source chain receives the packet over
   IBC/TAO and at the same time executes "Cash.Move" to transfer a Cash
   state to a recipient on the source chain.

Detailed specification
~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

No additional state class

Commands
^^^^^^^^

.. class:: HandleTransfer(msg: MsgTransfer)
   :noindex:

   A command executing token transfer specified in ICS-20

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel
      -  [Multiple] Cash(owner = senderUser)

   .. attribute:: Output states

      -  IbcChannel(packets += Pair(sequence, packet), nextSequenceSend
         += 1)
      -  [Multiple] Cash(owner = bankUser)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId.
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  The contents of the packet must be consistent with Cash owner,
         denomination and amount.
      -  Requirements for createOutgoingPacket specified in ICS-20 must
         be satisfied.

.. class:: HandlePacketRecv(msg: MsgRecvPacket)
   :noindex:

   A command receiving a token, specified in ICS-20

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel
      -  [Multiple] Cash(owner = bankUser)

   .. attribute:: Output states

      -  IbcChannel
      -  [Multiple] Cash(owner = receiverUser)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId.
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  The contents of the packet must be consistent with Cash owner,
         denomination and amount.
      -  Requirements for onRecvPacket specified in ICS-20 and
         recvPacket specified in ICS-4 must be satisfied.

.. class:: HandlePacketAcknowledgement(msg: MsgAcknowledgement)
   :noindex:

   A command receiving ack to a token, specified in ICS-20

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel
      -  [Multiple] Cash(owner = bankUser)

         -  Cash is needed only in the case of refund.

   .. attribute:: Output states

      -  IbcChannel(packets -= sequence, nextSequenceAck +=1)

         -  nextSequenceAck is incremented only when the channel is type
            of ORDERED.

      -  [Multiple] Cash(owner = senderUser)

         -  Cash is needed only in the case of refund.

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId.
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  The contents of the packet must be consistent with Cash owner,
         denomination and amount.
      -  Requirements for onAcknowledgePacket specified in ICS-20 and
         acknowledgePacket specified in ICS-4 must be satisfied.

Design of ICS-20 for Corda (when defining new asset type for ICS-20)
--------------------------------------------------------------------

Key point
~~~~~~~~~

-  Defining new asset type enables token lock, unlock, mint and burn in
   conjunction with IBC/TAO, without trusting in a particular node.
-  Hereinafter, this asset is referred to as ICS20Cash state.
-  The state responsible for locking, unlocking, minting and burning
   ICS20Cash is referred to as Bank state.

Security assumption
~~~~~~~~~~~~~~~~~~~

-  It is assumed that an issuer of ICS20Cash tokens issues them properly
   in the source chain. (trust in the issuer)

Procedure
~~~~~~~~~

1. Src: The sender sends a packet over IBC/TAO and at the same time
   locks his ICS20Cash state in the Bank state.

   -  TX inputs

      -  Bank: The lock balance of ICS20Cash is N.
      -  ICS20Cash: quantity is M, owner is the sender.

   -  TX outputs

      -  Bank: The lock balance of ICS20Cash becomes N + M.
      -  ICS20Cash: None(not included in outputs)

2. Dst: The IBC module receives the packet over IBC/TAO and at the same
   time mints a new ICS20Cash state to a recipient on the destination chain.

   -  TX inputs

      -  Bank: The mint balance of ICS20Cash is N.

   -  TX outputs

      -  Bank: The mint balance of ICS20Cash becomes N + M.
      -  ICS20Cash: Quantity is M and owner is the receiver on the
         destination chain.

3. Dst: The minted ICS20Cash can be arbitrarily transfered on the
   destination chain.
4. Dst: The sender sends a packet over IBC/TAO and at the same time
   burns his ICS20Cash state.

   -  TX inputs

      -  Bank: The mint balance of ICS20Cash is N + M.
      -  ICS20Cash: quantity is M and owner is the sender on the
         destination chain.

   -  TX outputs

      -  Bank: The mint balance of ICS20Cash is N
      -  ICS20Cash: None (not included in outputs)

5. Src: The IBC module receives the packet over IBC/TAO and at the same
   time unlocks a ICS20Cash state to a recipient on the source chain.

   -  TX inputs

      -  Bank: The lock balance of ICS20Cash is N + M'.

   -  TX outputs

      -  Bank: The lock balance of ICS20Cash becomes N.
      -  ICS20Cash: quantity is M' and owner is the recipient on the
         source chain.

Detailed specification
~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: ICS20Cash
   :noindex:

   The state class of token dedicated for ICS-20

   .. attribute:: amount
      :type: Amount<Issued<Currency>>

      The number of tokens. It also holds the issuer and units
      information as data types.

   .. attribute:: owner
      :type: AbstractParty

      Owner of the token state

   .. attribute:: participants
      :type: List<AbstractParty>

      Participants in the token state

.. class:: Bank
   :noindex:

   The state class responsible for locking, unlocking, minting and
   burning ICS20Cash

   .. attribute:: participants
      :type: List<AbstractParty>

   .. attribute:: baseId
      :type: StateRef

   .. attribute:: locked
      :type: Map<Denom, Amount>

      Locked amount for each denomination

   .. attribute:: minted
      :type: Map<Denom, Amount>

      Minted amount for each denomination

   .. attribute:: denoms
      :type: Map<Denom, Denom>

      Associative array used to support "IBC denom" specified in ADR-001
      of Cosmos.

Commands
^^^^^^^^

.. class:: HandleTransfer(msg: MsgTransfer)
   :noindex:

   A command class transfering ICS20Cash

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel
      -  Bank
      -  [Multiple] ICS20Cash(owner = senderUser)

   .. attribute:: Output states

      -  IbcChannel(packets += Pair(sequence, packet), nextSequenceSend
         += 1)
      -  Bank(locked + = Pair (denom, amount) or mined - = Pair (denom,
         amount))

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId.
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  The contents of the packet must be consistent with ICS20Cash
         owner, denomination and amount.
      -  Requirements for createOutgoingPacket specified in ICS-20 must
         be satisfied.

.. class:: HandlePacketRecv(msg: MsgRecvPacket)
   :noindex:

   A command class receiving ICS20Cash

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel
      -  Bank

   .. attribute:: Output states

      -  IbcChannel
      -  Bank(mined + = Pair(denom, amount) or locked -= Pair (denom,
         amount))
      -  ICS20Cash(owner = receiverUser)

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  The contents of the packet must be consistent with the
         ICS20Cash owner, denomination and amount
      -  Requirements for onRecvPacket specified in ICS-20 and
         recvPacket specified in ICS-4 must be satisfied.

.. class:: HandlePacketAcknowledgement(msg: MsgAcknowledgement)
   :noindex:

   A command class receiving acknowledgement for token transfer
   specified in ICS-20

   .. attribute:: Input states

      -  [readonly] Host
      -  [readonly] ClientState
      -  [readonly] IbcConnection
      -  IbcChannel
      -  Bank

         -  The Bank state is needed used only in the case of refund.

   .. attribute:: Output states

      -  IbcChannel(packets -= sequence, nextSequenceAck +=1)

         -  nextSequenceAck is incremented only when the channel is type
            of ORDERED.

      -  Bank(locked -= Pair (denom, amount) or minted += Pair (denom,
         amount))
      -  ICS20Cash(owner = senderUser)

         -  The ICS20Cash state is needed used only in the case of
            refund.

   .. attribute:: Contract rules

      -  All input and output states must have the same baseId.
      -  All input states (except readonly input called "reference
         state") must also be included in transaction output (because
         states of IBC are never deleted once created).
      -  The contents of the packet must be consistent with the
         ICS20Cash owner, denomination and amount
      -  Requirements for onAcknowledgePacket specified in ICS-20 and
         acknowledgePacket specified in ICS-4 must be satisfied.
