Introduction of Corda
=====================

Corda Overview
--------------

Corda_ is an opensource Distribute Ledger Technology (DLT) platform
developed by R3_.
Developed for enterprise, it has strengths in privacy and scalability.

.. _Corda: https://docs.corda.net/
.. _R3: https://www.r3.com/

Permissioned network
--------------------

-  Corda works by forming a permissioned network consisting of
   participating nodes and some TTP(Trusted Third Party) nodes.
-  TTP

   Network Map Service
      Manages node's location on the network

   Root CA
      Manages certificates for nodes' public keys

   Notary
      In Corda, which uses the UTXO model, validity of output
      consumption is notarized to prevent double consumption.

Asymmetric ledgers
------------------

-  In Corda, like a general blockchain, a ledger of each node is updated
   as transactions are proposed, validated and applied.
-  On the other hand, unlike a general blockchain, only nodes that
   participate in the transaction are involved in processing the
   transaction (for example, in the case of payment, the source and
   destination of payment), and even existence of the transaction is not
   disclosed to other nodes.
-  Each node's ledger consists of the result of applying only the
   transactions in which that node is a participant. As a result, each
   node's ledger generally has different contents from each other.
-  Therefore, nodes use P2P communication instead of broadcast
   communication (for example, gossip protocol) utilized in typical
   blockchains.

Transaction flow
----------------

-  The nodes confirm a transaction through the following flow and update
   their own ledgers.

   #. The first node, called an initiator, creates the transaction and
      signs it using the node's own private key.
   #. The initiator node asks responder nodes, who participate in the
      transaction, to verify the content of it and to sign it.
   #. The responder node verifies the following aspects of the
      transaction, signs the transaction and returns it to the initiator
      node if OK.

      -  Transaction integrity (described in the next section)
      -  Whether to accept the transaction (from a business point of
         view)

   #. The initiator node asks the notary to verify that the transaction
      causes no double consumption and to sign it.
   #. The notary verifies that the transaction is not double-consuming,
      and if OK, signs the transaction and sends it to participating
      nodes.
   #. The initiator distributes to the parties the transaction signed by
      all the required signers and the notary.

Transaction model
-----------------

-  Corda has adopted the following UTXO model

   Transaction input
      -  A reference to an output state to be consumed, which consists of a transaction id and an output index

   Transaction output
      -  An arbitrary JVM object that implements the ContractState
         interface defined in Corda

   Conditions for consumption of outputs
      -  Conditions are defined in the form of a JVM class that
         implements the Contract interface defined in Corda.

-  A transaction generally consists of the following components.

   -  Zero or more input states
   -  One or more output states
   -  Contracts to which the input and output states belong
   -  Commands
   -  Notary (optional)

-  (Input or output) State

   -  Instances of classes that implement the
      net.corda.core.contracts.ContractState interface
   -  Each state class corresponds to only one Contract class

-  Contract

   -  Classes that implement the net.corda.core.contracts.Contract
      interface
   -  Contracts define the way to verify transactions by implementing
      the "verify" method
   -  A transaction must pass all the validation defined by the verify
      methods of the child classes of Contract that correspond to all
      input and output states contained in the transaction.
   -  A transaction contains the classes derived from Contract to be
      used and all the code the classes depends on in the form of jar.

-  Command

   -  Instances of classes that extend the
      net.corda.core.contracts.Command class

   -  It consists of the following two elements.

      CommandData
         -  Data which specify the purpose of transaction execution (in
            other words, relationship between input and output states)
         -  For example, the Cash contract has three types of
            CommandData, Cash.Issue, Cash.Move and Cash.Exit.
         -  The verify method of classes derived from Contract verifies
            the transaction based on the specified CommandData.

      Signers
         -  Data which assign parties who must sign the transaction
         -  For example, if a transaction contains the Command of which
            CommandData is Cash.Move, all the owners of the Cash states
            included in the transaction as input are supposed to be
            assigned as signers of the Command. Otherwise the
            transaction is regarded invalid by the Cash::verify method.

Notarisation (Finality)
-----------------------

-  Purpose of Notary

   -  Because Corda uses the UTXO model, if one or more input states in
      a transaction have been consumed by other transactions, the
      transaction will be invalid (double consumption).
   -  Veritying a transaction within the parties of the transaction
      doesn't make sure that the transaction is not double-consuming.
   -  Since multiple transactions might attempt to consume the same
      state, a trusted third party is needed to determine which
      transaction has consumed the state first.
   -  Notary plays this role.
   -  The notary ensures all input states in a transaction have not been
      consumed in the past and signs the transaction only if double
      consumption doesn't occur. To do so, the notary maintains the
      record of the states that have been consumed.

-  Validating notary and non-validating notary

   -  Since the original purpose of notarization is only to prevent
      double consumption, it is not necessary to disclose any content
      other than the input states of the transaction to notary.
   -  Each notary can be configured to select one of the following two
      types of behavior.

      -  Validating notary

         -  Verifying not only that there is no double consumption but
            also validity of the transaction by checking that it
            satisfies all the conditions defined by the Contract classes
            included in it and is signed by all the signers required by
            the Command objects included in it.
         -  This type of notaries has the advantage that the transaction
            is assured to be valid only by the notary signature.

      -  Non-validating notary

         -  Verifying only that there is no double consumption
         -  This type of notaries has the advantage that elements other
            than input states of a transaction can be hidden from the
            notary.
         -  It is assumed that transaction is validated by parties to
            the transaction.

   -  In this way, notaries have different behavior, and in particular,
      validating notaries can see the entire contents of a transaction.
      In general, multiple notaries can operate on the same network,
      each transaction can choose and designate one of them from the
      point of view of privacy.

Updates and queries for ledgers (RPC)
-------------------------------------

-  Clients can execute the following operations on nodes through the
   Corda RPC

   Updating ledger = Executing flow
      -  Flow is definition of a transaction processing from initiation
         to notarization, in the form of program code.
      -  Clients can order the node to initiate a new flow through the
         Corda RPC and receive the result.

   Querying (unconsumed) states from ledger
      -  Clients can ask a node to search UTXOs of transactions to which
         the node is a party by several conditions and to return it.

Examples of applications of Corda
---------------------------------

Cash contract
~~~~~~~~~~~~~

States
^^^^^^

.. class:: Cash.State
   :noindex:

   State class representing cash

   .. attribute:: amount
      :type: Amount<Issued<Currency>>

      Represents a quantity, an issuer and currency unit of this cash

   .. attribute:: exitKeys
      :type: Collection<PublicKey>

      Represents keys required to execute the Cash.Exit command on
      this cash. This field comes from the FungibleAsset interface,
      which is implemented by the Cash.State class, and consists of
      an owner key and an issuer key.

   .. attribute:: owner
      :type: AbstractParty

      Represents an owner of this cash

   .. attribute:: participants
      :type: List<AbstractParty>

      Represents parties to this cash, in other words, those who
      store this state in their ledger. This field comes from the
      ContractState interface, which is implemented by the
      Cash.State class, and consists of an owner only.

Commands
^^^^^^^^

.. class:: Cash.Issue
   :noindex:

   A command allowing new cash states to be issued

   .. attribute:: Input states

      -  None

   .. attribute:: Output states

      -  Cash.State(owner = issuer)

   .. attribute:: Required signers

      -  The issuer of the Cash.State

.. class:: Cash.Move

   A command transfering ownership of cash states

   .. attribute:: Input states

      -  [Multiple] Cash.State(owner = <original owner>)

   .. attribute:: Output states

      -  [Multiple] Cash.State(owner = <next owner>)

   .. attribute:: Required signers

      -  The original owner of the Cash.State

   .. attribute:: Contract rules

      -  The sums of amounts, including issuer and currency, of input
         and output must equal to each other (except for amounts of
         "exited" Cash described below).

.. class:: Cash.Exit(amount: Amount<Issued<Currency>>)
   :noindex:

   A command withdrawing cash states from ledgers of Corda

   .. attribute:: Input states

      -  [Multiple] Cash.State

   .. attribute:: Output states

      -  [Multiple] Cash.State

   .. attribute:: Required signers

      -  The owner of the input side Cash.State
      -  The issuer of the input side Cash.State

   .. attribute:: Contract rules

      -  The difference between the sums of the amounts of input and
         output must equal to the amount attribute specified for his
         command.

CommercialPaper(CP) contract
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: CommercialPaper.State
   :noindex:

   State class representing the CP

   .. attribute:: faceValue
      :type: Amount<Issued<Currency>>

      Represents a face value of this CP

   .. attribute:: issuance
      :type: PartyAndReference

      Represents an issuer of this CP and additional information

   .. attribute:: maturityDate
      :type: Instant

      Represents a maturity date of this CP

   .. attribute:: owner
      :type: AbstractParty

      Represents an owner of this CP

   .. attribute:: participants
      :type: List<AbstractParty>

      Represnets parties to this CP, in other words, those who store
      this state in their ledger. This field comes from the
      ContractState interface, which is implemented by the
      CommercialPaper.State class, and consists of an owner only.

Commands
^^^^^^^^

.. class:: CommercialPaper.Issue
   :noindex:

   A command allowing new commercial papers to be issued

   .. attribute:: Input states

      -  None

   .. attribute:: Output states

      -  CommercialPaper.State (owner = issuer)

   .. attribute:: Required signers

      -  The issuer of the CommercialPaper.State

.. class:: Move
   :noindex:

   A command transfering ownership of a commercial paper

   .. attribute:: Input states

      -  CommercialPaper.State(owner = <original owner>)

   .. attribute:: Output states

      -  CommercialPaper.State(owner = <next owner>)

   .. attribute:: Required signers

      -  The original owner of CommercialPaper.State

   .. attribute:: Contract rules

      -  The attributes of the commercial paper other than owner (such
         as faceValue) remain unchanged.

.. class:: Redeem
   :noindex:

   A command redeeming a commercial paper

   .. attribute:: Input states

      -  CommercialPaper.State(faceValue, issuer, owner)
      -  [Multiple] Cash.State(owner = <issuer of CP>, amount =
         <faceValue of CP>)

   .. attribute:: Output states

      -  [Multiple] Cash.State(owner = <owner of CP>, amount =
         <faceValue of CP>)

   .. attribute:: Required signers

      -  The owner of the the CommercialPaper.State (to allow the
         redemption of the commercial paper)
      -  The issuer of the CommercialPaper.State (to allow the transfer
         of the cash states)

   .. attribute:: Contract rules

      -  The CommercialPaper.State is not included in output and removed
         from ledgers.
      -  The cash states equivalent to to the face value of the CP is
         transfered from the issuer of the CP to the owner of the CP (in
         exchange for the CP).
