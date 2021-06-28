ICS-20 for Corda
================

ICS-20概要
-----------

`ICS-20 <https://github.com/cosmos/ibc/blob/master/spec/app/ics-020-fungible-token-transfer/README.md>`_
はIBC仕様の一部であり、
ブロックチェーン間のトークン転送を実現するIBC/APPプロトコルである。
パケット送受信とトークンのlock/unlock/burn/mint処理を同時実行することでブロックチェーン間のトークン送受信を実現する。
その手順は以下のとおりである。

1. Src: IBCパケット送信 + senderからescrowへトークンをlock
2. Dst: IBCパケット受信 + escrowからreceiverへバウチャートークンをmint
3. Dst: バウチャートークンを任意に所有権移転
4. Dst: IBCパケット送信 + sender'のバウチャートークンをburn
5. Src: IBCパケット受信 + escrowからreceiver'へトークンをunlock

ICS-20 for Corda設計(既存のCash stateを用いる場合)
------------------------------------------------------------------

要点
~~~~~~~~~

- ICS-20では、escrowの役割を、特定のユーザではなく中立的機械であるスマートコントラクトに負わせることが前提になっている。
- しかしCordaの既存asset(e.g. Cash)は、特定のユーザ(Node)によって所有されることしか想定されておらず、スマートコントラクトにlockしたりmintさせることは難しい。
- そこで特定のNode(金融機関等)を信頼し、そのNodeにescrowの役割を演じてもらう。

  - 以下ではこのNodeをBankと呼ぶこととする。

セキュリティ仮定
~~~~~~~~~~~~~~~~~~~

- Srcチェーンにおいては、Cash発行は発行元によって適切に実施されているものとする(発行元への信頼)
- Bank nodeはICS-20を通じてのみCashをlock(move)/unlock(move)/mint(issue)/burn(exit)するものとする(Bank nodeへの信頼)

手順
~~~~~~~~~

1. Src: IBCパケット送信 + senderからBank@SrcへCash@Srcをmove
2. Dst: IBCパケット受信 + Bank@DstからreceiverへCash@Dstをissue+move
3. Dst: Cash@Dstの任意のmove
4. Dst: IBCパケット送信 + sender'からBank@DstへCash@Dstをmove+exit
5. Src: IBCパケット受信 + Bank@Srcからreceiver'へCash@Srcをmove

詳細仕様
~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

追加state class無し

Commands
^^^^^^^^

.. class:: HandleTransfer(msg: MsgTransfer)
   :noindex:

   ICS-20送金を指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel
      - [複数可] Cash(owner = senderUser)

   .. attribute:: Output states

      - IbcChannel(packets += Pair(sequence, packet), nextSequenceSend += 1)
      - [複数可] Cash(owner = bankUser)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - packetの内容がCashの内容(ownerやdenominationやamount)と整合すること
      - その他ICS-20のcreateOutgoingPacketで規定されているルールに準拠すること

.. class:: HandlePacketRecv(msg: MsgRecvPacket)
   :noindex:

   ICS-20受信を指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel
      - [複数可] Cash(owner = bankUser)

   .. attribute:: Output states

      - IbcChannel
      - [複数可] Cash(owner = receiverUser)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - packetの内容がCashの内容(ownerやdenominationやamount)と整合すること
      - その他ICS-20のonRecvPacket及びICS-4のrecvPacketで規定されているルールに準拠すること

.. class:: HandlePacketAcknowledgement(msg: MsgAcknowledgement)
   :noindex:

   ICS-20 ACK受信を指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel
      - [複数可] Cash(owner = bankUser)

        - 送金先でパケット受信が失敗した場合のみ返金のためCashを処理する

   .. attribute:: Output states

      - IbcChannel(packets -= sequence, nextSequenceAck +=1)

        - ORDERED channelの場合のみnextSequenceAckをインクリメント

      - [複数可] Cash(owner = senderUser)

        - 送金先でパケット受信が失敗した場合のみ返金のためCashを処理する

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - packetの内容がCashの内容(ownerやdenominationやamount)と整合すること
      - その他ICS-20のonAcknowledgePacket及びICS-4のacknowledgePacketで規定されているルールに準拠すること

Corda-IBCでの実装(ICS20専用のassetを定義する場合)
--------------------------------------------------------------------

要点
~~~~~~~~~

- 専用のassetであればIBC/TAOと連動してトークンをlock/unlock/mint/burnさせることができ、特定のNodeを信頼せずに済む。
- 以下ではこのassetをICS20Cashと呼ぶこととする。
- ICS20Cashのlock/unlock/mint/burnの管理を担うStateをBank stateと呼ぶことにする。

セキュリティ仮定
~~~~~~~~~~~~~~~~~~~

- Srcチェーンにおいては、ICS20Cash発行は発行元によって適切に実施されている前提とする(発行元への信頼)

手順
~~~~~~~~~

1. Src: IBCパケット送信 + senderからBank@SrcへICS20Cash@Srcをlock

   - TX inputs

     - Bank: ICS20Cashのlock残高はN
     - ICS20Cash: 額面はM、所有者はsender@Src

   - TX outputs

     - Bank: ICS20Cashのlock残高はN + M
     - ICS20Cash: None(outputsに含めない)

2. Dst: IBCパケット受信 + Bank@DstからreceiverへICS20Cash@Dstをmint

   - TX inputs

     - Bank: ICS20Cashのmint残高はN

   - TX outputs

     - Bank: ICS20Cashのmint残高はN + M
     - ICS20Cash: 額面はM、所有者はreceiver@Dst

3. Dst: ICS20Cash@Dstの任意のmove
4. Dst: IBCパケット送信 + sender'からBank@DstへICS20Cash@Dstをburn

   - TX inputs

     - Bank: ICS20Cashのmint残高はN + M
     - ICS20Cash: 額面はM、所有者はsender'@Dst

   - TX outputs

     - Bank: ICS20Cashのmint残高はN
     - ICS20Cash: None(outputsに含めない)

5. Src: IBCパケット受信 + Bank@Srcからreceiver'へICS20Cash@Srcをunlock

   - TX inputs

     - Bank: ICS20Cashのlock残高はN + M'

   - TX outputs

     - Bank: ICS20Cashのmint残高はN
     - ICS20Cash: 額面はM'、所有者はreceiver'@Src

詳細仕様
~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: ICS20Cash
   :noindex:

   ICS-20の送金に対応したトークンstate class

   .. attribute:: amount
      :type: Amount<Issued<Currency>>

      トークンの数量。またデータ型としてissuer(発行者)及びdenomination(単位)の情報も保持する。

   .. attribute:: owner
      :type: AbstractParty

      トークンの所有者

   .. attribute:: participants
      :type: List<AbstractParty>

      トークンの当事者

.. class:: Bank
   :noindex:

   ICS20Cashのlock/unlock/mint/burnを管理するstate class

   .. attribute:: participants
      :type: List<AbstractParty>

   .. attribute:: baseId
      :type: StateRef

   .. attribute:: locked
      :type: Map<Denom, Amount>

      denom別のlock済み金額

   .. attribute:: minted
      :type: Map<Denom, Amount>

      denom別のmint済み金額

   .. attribute:: denoms
      :type: Map<Denom, Denom>

      cosmos-sdkのdenomの扱い(ics/xxxxxxx)に対応するための連想配列

Commands
^^^^^^^^

.. class:: HandleTransfer(msg: MsgTransfer)
   :noindex:

   ICS-20送金を指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel
      - Bank
      - [複数可] ICS20Cash(owner = senderUser)

   .. attribute:: Output states

      - IbcChannel(packets += Pair(sequence, packet), nextSequenceSend += 1)
      - Bank(locked += Pair(denom, amount) または minted -= Pair(denom, amount))

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - packetの内容がICS20Cashの内容(ownerやdenominationやamount)と整合すること
      - その他ICS-20のcreateOutgoingPacketで規定されているルールに準拠すること

.. class:: HandlePacketRecv(msg: MsgRecvPacket)
   :noindex:

   ICS-20受信を指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel
      - Bank

   .. attribute:: Output states

      - IbcChannel
      - Bank(minted += Pair(denom, amount) または locked -= Pair(denom, amount))
      - ICS20Cash(owner = receiverUser)

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - packetの内容がICS20Cashの内容(ownerやdenominationやamount)と整合すること
      - その他ICS-20のonRecvPacket及びICS-4のrecvPacketで規定されているルールに準拠すること

.. class:: HandlePacketAcknowledgement(msg: MsgAcknowledgement)
   :noindex:

   ICS-20 ACK受信を指示するcommand class

   .. attribute:: Input states

      - [readonly] Host
      - [readonly] ClientState
      - [readonly] IbcConnection
      - IbcChannel
      - Bank

        - 送金先でパケット受信が失敗した場合のみ返金のためBank/ICS20Cashを処理する

   .. attribute:: Output states

      - IbcChannel(packets -= sequence, nextSequenceAck +=1)

        - ORDERED channelの場合のみnextSequenceAckをインクリメント

      - Bank(locked -= Pair(denom, amount) または minted += Pair(denom, amount))
      - ICS20Cash(owner = senderUser)

        - 送金先でパケット受信が失敗した場合のみ返金のためBank/ICS20Cashを処理する

   .. attribute:: Contract rules

      - 全てのInput/Output statesが同一のbaseIdを共有していること
      - 全てのInput states(readonlyを除く)がOutput statesにも含まれること(IBCのstateは作成後に消去されることはないため)
      - packetの内容がICS20Cashの内容(ownerやdenominationやamount)と整合すること
      - その他ICS-20のonAcknowledgePacket及びICS-4のacknowledgePacketで規定されているルールに準拠すること
