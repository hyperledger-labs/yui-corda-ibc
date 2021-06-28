IBC/TAO for Corda
=================

IBC概要
-------

Inter-Blockchain Communication(IBC_)プロトコルは
異種のブロックチェーン同士でデータ仲介者(Relayer)を信頼することなく
通信するためのプロトコルです。
このプロトコルは以下の2レイヤーから成ります。

.. _IBC: https://github.com/cosmos/ibc

TAO(Transport, Authentication and Ordering layer)プロトコル
  ブロックチェーン間の通信(ハンドシェークとパケット送受信)を
  実現するプロトコル

APP(APPlication layer)プロトコル
  IBC/TAOを利用してクロスチェーンのアプリケーションを実現する
  プロトコル群の総称

IBC/TAOでは以下の手順でパケットが送受信されます。

#. Srcチェーン: パケット送信
#. Dstチェーン: パケット受信 + ACK返信

   - Srcチェーンが確実にパケット送信処理を実行したことを検証

#. Srcチェーン: ACK受信

   - Dstチェーンが確実にパケット受信処理を実行したことを検証

一方でIBC/APPによって以下の手順でクロスチェーンアプリケーションが
動作します。

#. Srcチェーン: パケット送信 + **送信時APP処理**
#. Dstチェーン: パケット受信 + **受信時APP処理** + ACK返信

   - Srcチェーンが確実にパケット送信処理を実行したことを検証

#. Srcチェーン: ACK受信 + **ACK受信時APP処理**

   - Dstチェーンが確実にパケット受信処理を実行したことを検証

両者を比較すると、ブロックチェーン間の相互運用性の
仕組み(ハンドシェーク・パケット送受信)はIBC/TAOで完結している
ことが分かります。
両ブロックチェーン上でIBC/TAOのパケット送受信の処理に
IBC/APPの処理を連動させるだけで、多様なアプリケーションの相互運用が
実現できることがIBCの特長です。

IBCを実行するための要件
------------------------

IBCは `ICS-24`_ でプラットフォームの詳細な要件を規定していますが、
大まかにIBCは以下のような性質をプラットフォームに要求します。

.. _`ICS-24`: https://github.com/cosmos/ibc/tree/master/spec/core/ics-024-host-requirements

- IBCが規定する状態データ(ClientState, ConsensusState, Connection, Channel, PacketCommitment, Acknowledgement等)が改ざん不能な形で記録できること
- IBCが規定する状態データが、IBCが規定する規則に従い正しく状態遷移できること
- IBCが規定する状態データが、IBCが規定する規則以外の方法での変更・消去が不可能であること
- IBCが規定する状態データを、IDによって一意に指定して取得可能であること
- 取得した状態データの正しさを、プラットフォームの外で、適切なLight clientを用いて検証できること

Corda-IBCのアプローチ
---------------------

- Corda-IBCは、Cordaネットワーク同士あるいはCordaネットワークと他のブロックチェーンをIBCを用いて接続するためのフレームワークである
- しかしCordaにおけるネットワーク/台帳は、それ自体はIBCが求める要件を満たしたプラットフォームにはなっていない
- そこでCorda-IBCでは、Cordaによるネットワーク/台帳の全体をIBCによって接続する対象として扱うのではなく、Corda上で動作するアプリケーション(CorDapp)の内部にIBCに適合する状態と状態遷移を構成し、これをIBCによって他のブロックチェーンと接続する

課題: IDによる状態データの一意な指定
-------------------------------------

- IBCは各種stateをIDによって一意に指定して取得可能であることを要求する
- これに対してCordaにおけるstateは単純にIDによって一意に識別することは困難
- もちろん或る一つのstateが不変なIDを持つようにすることは容易

  - 例えばstate生成時にIDを付与する
  - 以降の状態遷移ではIDが変化しないようcontractで強制すればよい

- しかしCordaではネットワーク上の全てのstate間の整合性を保証するような仕組みが無いため、同じcontractに従う複数のstateが同じIDを持たないよう強制することは難しい

解決方式: Genesis-Host方式
-----------------------------

- 概要

  - まずGenesisというstateを生成し、続いてこれを消費して代わりにHostという状態を生成する
  - この時にHostのIDの一部として、Genesisを含むトランザクションのtxidを埋め込む
  - notaryが二重消費の防止によってGenesisが唯一つのトランザクションにより消費されることを保証するため、これによりHostのIDの一意性も保証される
  - Hostを起点としてIBCの各種stateを生成することで、これらstateのIDの一意性を強制する
  - このHostが、Corda-IBCによって、IBCプロトコルを用いて他のブロックチェーンと接続される対象となる

- Genesis state

  - HostのIDの一意性を得るために利用されるstate
  - Genesisを含むトランザクションのtxidは"baseID"としてHostに設定される
  - 一つのnotaryの下で同じtxidを持つ複数のトランザクションを消費することは出来ないため、或るbaseIDを持つHostは唯一つであることが保証される

- Host state

  - IBCの各種stateのID発行を集中管理するstate
  - IBCの各種stateは全ていずれかのHostから派生して(Hostをinputに持つトランザクションを通じて)生成する
  - これら全てのstateに同じbaseIDが埋め込まれるため、異なるHostに属するstateは(例えstate IDが同一でも)区別できる

- Genesis-Host方式におけるIDの一意性

  - notaryは公開鍵によって一意に識別できる
  - notaryのスコープにおいて、HostはbaseIDによって一意に識別できる
  - Hostのスコープにおいて、各stateはIDによって一意に識別できる
  - したがって、notary公開鍵+baseID+stateIDの組によって、全てのIBCのstateは一意に識別できる

Corda-IBCに対するLight client
------------------------------

- Light clientの内部データ

  - Notary公開鍵

    - 対象Notaryを一意に識別するため

  - baseID

    - 対象Hostを一意に識別するため

      - 当該HostをLight client対象の「ブロックチェーン」と見做す。

- Stateの検証方法

  - Proof = 検証対象stateをOutputに持つ署名済トランザクション
  - 検証手順

    #. 当該StateがトランザクションのOutputとして含まれることを検証
    #. 当該Stateが所定baseIDを持つことを検証
    #. トランザクションが所定Notaryに署名されていることを検証

制限: パケット受信タイムアウトの扱い
-------------------------------------

- IBCではパケット送信の際、height単位もしくはtimestamp単位でのパケット有効期限を設定可能な仕様になっており、この有効期限を過ぎてからパケット受信をしても失敗する
- Corda-IBCではheightやtimestampを定義していないため、Cordaがパケットの受信側として動作する場合、受信タイムアウトを扱うことはできない。

IBC/TAO for Corda詳細仕様
--------------------------

Ibc contract: Host part
~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: Genesis
   :noindex:

   HostのbaseIDを規定するためのstate class

   .. attribute:: participants
      :type: List<AbstractParty>

      本Genesis/Host及びそこから派生する全てのstateの共有先となるユーザのリスト

.. class:: Host
   :noindex:

   Hostを表現するstate class

   .. attribute:: participants
      :type: List<AbstractParty>

      Genesisから引き継いだ(同一内容の)participants

   .. attribute:: baseId
      :type: StateRef

      Genesisのreference(= txhash + output index)

   .. attribute:: notary
      :type: Party

      Genesisをnotarizeするnotary

   .. attribute:: clientIds
      :type: List<Identifier>

      Host配下で作成したClientStateのIDのリスト。
      同一のIDを振り出さないようにするために使う。

   .. attribute:: connIds
      :type: List<Identifier>

      Host配下で作成したConnectionのIDのリスト。
      同一のIDを振り出さないようにするために使う。

   .. attribute:: portChanIds
      :type: List<Pair<Identifier, Identifier>>

      Host配下で作成したChannelのportID及びchannelIDのペアのリスト。
      同一のIDペアを振り出さないようにするために使う。
      異なるportで同一のchannelIDが使われることは許容する。

Commands
^^^^^^^^

.. class:: GenesisCreate
   :noindex:

   Genesisの作成を指示するcommand class

   .. attribute:: Input states

      - None

   .. attribute:: Output states

      - Genesis

   .. attribute:: Contract rules

      - トランザクションに他のinput/output stateが含まれていないこと

.. class:: HostCreate
   :noindex:

   GenesisをconsumeしてHostを作成する指示のcommand class

   .. attribute:: Input states

      - Genesis

   .. attribute:: Output states

      - Host

   .. attribute:: Contract rules

      - Host.participants == Genesis.participants
      - Host.baseId == refOf(Genesis)
      - Host.notary == notaryOf(Genesis)

Ibc contract: Client part
~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: ClientState
   :noindex:

   ICS-2のClientStateを表現するstate class

   .. attribute:: participants
      :type: List<AbstractParty>

      Hostから引き継いだ(同一内容の)participants

   .. attribute:: baseId
      :type: StateRef

      Hostから引き継いだ(同一内容の)baseId

   .. attribute:: id
      :type: Identifier

      本ClientStateのidentifier

   .. attribute:: clientState
      :type: Any

      protobufでencodeされたClientStateの本体(対応チェーン毎に異なる)

   .. attribute:: consensusStates
      :type: Map<Height, ConsensusState>

      HandleClientCreate及びHandleClientUpdateによって与えられたheightとConsensusStateのペアの連想配列

Commands
^^^^^^^^

.. class:: HandleClientCreate(msg: MsgCreateClient)
   :noindex:

   ClientStateの作成を指示するcommand class

   .. attribute:: Input states

      - Host

   .. attribute:: Output states

      - Host(clientIds += 新ClientStateのID)
      - ClientState

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput statesがOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - HostのclientIdsに新ClientStateのIDが追加されていること
      - MsgCreateClientで与えられたClientState及びConsensusStateを用いて正しく作成したClientStateが出力になっていること

Ibc contract: Connection part
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: IbcConnection
   :noindex:

   ICS-3のConnectionを表現するstate class

   .. attribute:: participants
      :type: List<AbstractParty>

      Hostから引き継いだ(同一内容の)participants

   .. attribute:: baseId
      :type: StateRef

      Hostから引き継いだ(同一内容の)baseId

   .. attribute:: id
      :type: Identifier

      本Connectionのidentifier

   .. attribute:: end
      :type: ConnectionEnd

      protobufで自動生成したConnectionEnd

Commands
^^^^^^^^

.. class:: HandleConnOpenInit(msg: MsgConnectionOpenInit)
   :noindex:

   connOpenInitを指示するcommand class

   .. attribute:: Input states

      - Host
      - [readonly] ClientState

   .. attribute:: Output states

      - Host(connIds += 新ConnectionのID)
      - IbcConnection(end.state == INIT)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - HostのconnIdsに新IbcConnectionのIDが追加されていること
      - その他ICS-3のconnOpenInitで規定されているルールに準拠すること

.. class:: HandleConnOpenTry(msg: MsgConnectionOpenTry)
   :noindex:

   connOpenTryを指示するcommand class

   .. attribute:: Input states

      - Host
      - [readonly] ClientState
      - [optional] IbcConnection(end.state == INIT)

        - ICS-3のconnOpenTryで規定されている通り、予めINIT状態のIbcConnectionを作成済みでも未作成でも良い

   .. attribute:: Output states

      - Host(connIds += 新IbcConnectionのID)
      - IbcConnection(end.state == TRYOPEN)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - HostのconnIdsに新IbcConnectionのIDが追加されていること

        - 予めIbcConnection作成済みの場合は追加なし

      - その他ICS-3のconnOpenTryで規定されているルールに準拠すること

.. class:: HandleConnOpenAck(msg: MsgConnectionOpenAck)
   :noindex:

   connOpenAckを指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - IbcConnection(end.state == INIT or TRYOPEN)

        - ICS-3のconnOpenAckで規定されている通り、IbcConnection状態はINIT状態の場合とTRYOPEN状態の2通りが可能

   .. attribute:: Output states

      - IbcConnection(end.state == OPEN)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - ICS-3のconnOpenAckで規定されているルールに準拠すること

.. class:: HandleConnOpenConfirm(msg: MsgConnectionOpenConfirm)
   :noindex:

   connOpenConfirmを指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - IbcConnection(end.state == TRYOPEN)

   .. attribute:: Output states

      - IbcConnection(end.state == OPEN)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - ICS-3のconnOpenConfirmで規定されているルールに準拠すること

Ibc contract: Channel part
~~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: IbcChannel
   :noindex:

   ICS-4のChannelを表現するstate class

   .. attribute:: participants
      :type: List<AbstractParty>

      Hostから引き継いだ(同一内容の)participants

   .. attribute:: baseId
      :type: StateRef

      Hostから引き継いだ(同一内容の)baseId

   .. attribute:: id
      :type: Identifier

      本Channelのchannel identifier

   .. attribute:: portId
      :type: Identifier

      本Channelのport identifier

   .. attribute:: end
      :type: Channel

      protobufで自動生成したChannel

   .. attribute:: nextSequenceSend
      :type: Long

      次回の送信パケットに使用するsequence

   .. attribute:: nextSequenceRecv
      :type: Long

      (ORDEREDの場合のみ使用する)次回の受信予定パケットのsequence

   .. attribute:: nextSequenceAck
      :type: Long

      (ORDEREDの場合のみ使用する)次回の受信予定ACKのsequence

   .. attribute:: packets
      :type: Map<Long, Packet>

      送信済みパケットのsequenceによる連想配列

   .. attribute:: receipts
      :type: Set<Long>

      受信済みパケットのsequenceの集合

   .. attribute:: acknowledgements
      :type: Map<Long, Acknowledgement>

      送信済みACKのsequenceによる連想配列

Commands
^^^^^^^^

.. class:: HandleChanOpenInit(msg: MsgChannelOpenInit)
   :noindex:

   chanOpenInitを指示するcommand class

   .. attribute:: Input states

      - Host
      - [readonly] IbcConnection

   .. attribute:: Output states

      - Host(portChanIds += Pair(Channel.portId, Channel.id))
      - IbcChannel(end.state = INIT)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - HostのportChanIdsに新IbcChannelのportId/channelIdが追加されていること
      - その他ICS-4のchanOpenInitで規定されているルールに準拠すること

.. class:: HandleChanOpenTry(msg: MsgChannelOpenTry)
   :noindex:

   chanOpenTryを指示するcommand class

   .. attribute:: Input states

      - Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - [optional] IbcChannel(end.state = INIT)

        - ICS-4のchanOpenTryで規定されている通り、予めINIT状態のIbcChannelを作成済みでも未作成でも良い

   .. attribute:: Output states

      - Host(portChanIds += Pair(Channel.portId, Channel.id))
      - IbcChannel(end.state = TRYOPEN)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - HostのportChanIdsに新IbcChannelのportId/channelIdが追加されていること

        - 予めIbcChannel作成済みの場合は追加なし

      - その他ICS-4のchanOpenTryで規定されているルールに準拠すること

.. class:: HandleChanOpenAck(msg: MsgChannelOpenAck)
   :noindex:

   chanOpenAckを指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel(end.state = INIT or TRYOPEN)

        - ICS-4のchanOpenAckで規定されている通り、IbcChannel状態はINIT状態の場合とTRYOPEN状態の2通りが可能

   .. attribute:: Output states

      - IbcChannel(end.state = OPEN)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - その他ICS-4のchanOpenAckで規定されているルールに準拠すること

.. class:: HandleChanOpenConfirm(msg: MsgChannelOpenConfirm)
   :noindex:

   chanOpenConfirmを指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel(end.state = TRYOPEN)

   .. attribute:: Output states

      - IbcChannel(end.state = OPEN)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - その他ICS-4のchanOpenConfirmで規定されているルールに準拠すること

.. class:: HandleChanCloseInit(msg: MsgChannelCloseInit)
   :noindex:

   chanCloseInitを指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] IbcConnection
      - IbcChannel(end.state = OPEN)

   .. attribute:: Output states

      - IbcChannel(end.state = CLOSED)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - その他ICS-4のchanCloseInitで規定されているルールに準拠すること

.. class:: HandleChanCloseConfirm(msg: MsgChannelCloseConfirm)
   :noindex:

   chanCloseConfirmを指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel(end.state = OPEN)

   .. attribute:: Output states

      - IbcChannel(end.state = CLOSED)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - その他ICS-4のchanCloseConfirmで規定されているルールに準拠すること
