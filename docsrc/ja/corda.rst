Introduction of Corda
=====================

概要
----

Corda_ は R3_ がオープンソースで開発するDistributed Ledger Technology(DLT)のプラットフォームです。
エンタープライズ向けに開発され、プライバシーやスケーラビリティに強みがあります。

.. _Corda: https://docs.corda.net/
.. _R3: https://www.r3.com/

許可型ネットワーク
--------------------

- CordaはNode群及び幾つかのTTP(Trusted Third Party)群からなる許可型ネットワークを形成して動作する
- TTP

  Network Map Service
    各Nodeの物理情報を管理

  Root CA
    各Nodeの公開鍵に対する証明書を管理

  Notary
    UTXOモデルを採用するCordaにおいてoutputの正当な消費を公証して二重消費を防止

非対称な台帳
------------------

- Cordaは一般的なブロックチェーンと同様、各Nodeが台帳を持ち、ネットワークにトランザクションが提案され、検証され、適用されることを通じて更新される
- 一方で一般的なブロックチェーンと異なり、このトランザクションの処理に関与するのは当該取引の当事者Node達のみであり(e.g. 送金であれば送金元と送金先)、それ以外のNodeにはトランザクションの存在自体が開示されない
- 結果として各Nodeが持つ台帳は、当該Nodeが当事者となっているトランザクションのみを適用した結果として構成されるため、一般的にNode毎に異なった内容となる
- したがってNode間の通信も、一般的なブロックチェーンで行われる同報通信(e.g. gossipなど)ではなく、関係者間の一対一通信を用いる

トランザクションフロー
----------------

- トランザクションは以下の流れを経て確定し、当事者Nodeたちの台帳を更新する

  #. 最初のNode(initiator)がトランザクションを作成して自身で署名
  #. Initiator nodeは、署名が必要なNode(responder)群へトランザクション内容の検証及び署名を依頼
  #. Responder nodeはトランザクションの以下の点を検証してOKならば署名して返信

     - トランザクションの整合性を検証(詳細は次項で説明)
     - (ビジネス観点で)取引を受け入れるかどうか判断

  #. Initiator nodeは、Notaryへ二重消費の検証及び署名を依頼
  #. Notaryはトランザクションが二重消費をしていないことを検証してOKならば署名して返信
  #. Initiator nodeは、Responder群及びNotaryによる必要な全ての署名が付与されたトランザクションを、取引当事者へ配布。
    
トランザクションモデル
-----------------------

- CordaはUTXOモデルを採用している

  - BitcoinのUTXOモデル

    状態
      - BTCの数量

    消費条件
      - 消費対象outputが未消費(unspent)であること
      - 消費対象outputに埋め込まれたlocking scriptと整合するunlocking scriptが、消費側transactionに埋め込まれていること

        - 例: P2PKH(Pay-to-PubKey-Hash)

          - locking script

            - BTCの送付先アドレス(公開鍵のハッシュ値)

          - unlocking script

            - BTCの所有アドレスに対応する公開鍵及び署名

  - CordaのUTXOモデル

    状態
      - 一定のinterfaceを実装した任意のJVMオブジェクト

    消費条件
      - 一定のinterfaceを実装した任意のJVMクラスによって定義

- トランザクションは概して以下の構成要素から成る。

  - Input states(plural)
  - Output states(plural)
  - Contracts(plural)
  - Commands(plural)
  - Notaryの指定(optional)

- Input/Output state

  - net.corda.core.contracts.ContractStateインターフェースを実装したJVMクラスのインスタンス
  - 各Stateクラスは唯一つのContractクラスと対応

- Contract

  - net.corda.core.contracts.Contractインターフェースを実装したJVMクラス
  - Contract::verifyメソッドによってトランザクションを検証するロジックを定義する
  - トランザクションは、格納する全てのInput/Output stateに対応する、全てのContractクラスのverifyメソッドを通過できるものでなくてはならない。
  - トランザクションにはContractクラス及びそれが依存する全てのコードがjar形式で埋め込まれる。

- Command

  - net.corda.core.contracts.Commandクラスのインスタンス
  - 以下の2要素から成る。

    CommandData

      - 実行目的(入出力関係)の指定
      - 例えばCash contractはCommandDataとして3種類のCash.Issue(発行)/Cash.Move(移転)/Cash.Exit(回収)を持つ
      - Contract::verifyメソッドは指定されたCommandDataに応じたロジックでトランザクションを検証する

    Signers

      - 必要な署名者(複数)の指定
      - 例えばCommandDataがCash.Moveである場合、SignersとしてはInputとして指定された全てのCash.Stateの所有者を指定する必要が有る(指定しないとContract::verifyメソッドに弾かれる)

Notarisation (Finality)
-----------------------

- Notaryの目的

  - CordaはUTXOモデルを採用しているため、トランザクションに含まれる一つ以上のInput stateが他のトランザクションによって消費済みだと、トランザクションは不正となる(二重消費)
  - トランザクションの当事者内での検証だけでは、トランザクションが二重消費を起こしていないことを確定することはできない
  - 異なる複数の当事者群が並行してトランザクション処理を行う中で、最初に当該Input stateを消費したトランザクションを確定する、信頼できる第三者が必要
  - このような役割を担うのがNotary
  - Notaryはトランザクションに含まれる全てのInput statesを検証し(過去に消費されたOutput statesの記録と照合する)、二重消費が起きていない場合のみ、当該トランザクションに署名を付与する。

- Validating notaryとNon-validating notary

  - Notaryの目的は二重消費の防止であるため、トランザクションのInput states以外の内容をNotaryに開示する必要性は必ずしも無い。
  - 各Notaryは以下二種類のいずれかの振る舞いを選択する。

    - Validating notary

      - 二重消費が無いことを検証する
      - トランザクションのvalidity(Contractを満足し、必要な署名を揃えていること)も検証した上で署名する
      - トランザクションのvalidityがNotary署名のみで確定する利点がある

    - Non-validating notary

      - 二重消費が無いことだけを検証する
      - トランザクションのInput states以外の要素をNotaryに対して秘匿できる
      - トランザクションのvalidityは別途トランザクション当事者が検証することを想定する

  - このようにNotaryには挙動の違いがあり、特にValidating notaryにはトランザクション内容全部が開示されることもあり、一般的に同一のネットワークに複数の異なるNotaryを運用して選択することが可能であり、使用するNotaryの指定もトランザクションの構成要素になっている

台帳の更新と問い合わせ(RPC)
-------------------------------------

- Nodeに対してRPCを通じて以下の処理を実行できる

  更新(Flowの実行)
    - Flowとは、トランザクションフローをプログラムコードの形で定義したものである
    - RPC経由でFlowの開始を指示し、実行結果を受け取ることができる

  問い合わせ
    - 当該Nodeが当事者となったトランザクションのUTXOを幾つかの条件で検索して取得できる

Cordaアプリケーションの例
---------------------------------

Cash(現金) contract
~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: Cash.State
   :noindex:

   現金を表現するstate class

   .. attribute:: amount
      :type: Amount<Issued<Currency>>

      現金の数量、発行者、および通貨単位

   .. attribute:: exitKeys
      :type: Collection<PublicKey>

      本Cash.Stateに対してCash.Exitを実行するために署名が必要な鍵の一覧。
      Cash.Stateが実装しているFungibleAssetインターフェースに由来し、
      Cash.Stateにおいては発行者及び現所有者の鍵

   .. attribute:: owner
      :type: AbstractParty

      現所有者

   .. attribute:: participants
      :type: List<AbstractParty>

      本stateの当事者、つまり本stateを台帳に記録する者。
      Cash.Stateが実装しているContractStateインターフェースに由来し、
      Cash.Stateにおいては現所有者のみが該当

Commands
^^^^^^^^

.. class:: Cash.Issue
   :noindex:

   Cashの発行を指示するcommand class

   .. attribute:: Input states

      - None

   .. attribute:: Output states

      - Cash.State(owner = issuer自身)

   .. attribute:: Required signers

      - 当該Cash.Stateのissuer

.. class:: Cash.Move
   :noindex:

   Cashの所有権移転を指示するcommand class

   .. attribute:: Input states

      - [複数可] Cash.State(owner = 移転前の所有者)

   .. attribute:: Output states

      - [複数可] Cash.State(owner = 移転後の所有者)

   .. attribute:: Required signers

      - Input側のCash.Stateのowner

   .. attribute:: Contract rules

      - InputとOutputの双方の合計amountが(issuer, currencyを含め)一致すること(但し後述のExit分を除く)

.. class:: Cash.Exit(amount: Amount<Issued<Currency>>)
   :noindex:

   Cashの回収(消去)を指示するcommand class

   .. attribute:: Input states

      - [複数可] Cash.State

   .. attribute:: Output states

      - [複数可] Cash.State

   .. attribute:: Required signers

      - Input側のCash.Stateのowner
      - Input側のCash.Stateのissuer

   .. attribute:: Contract rules

      - InputとOutputの双方の合計amountの差が、本コマンドのamount属性と一致すること

CommercialPaper(CP) contract
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

States
^^^^^^

.. class:: CommercialPaper.State
   :noindex:

   CPを表現するstate class

   .. attribute:: faceValue
      :type: Amount<Issued<Currency>>

      額面

   .. attribute:: issuance
      :type: PartyAndReference

      発行者及び付随情報

   .. attribute:: maturityDate
      :type: Instant

      償還日

   .. attribute:: owner
      :type: AbstractParty

      現所有者

   .. attribute:: participants
      :type: List<AbstractParty>

      本stateの当事者、すなわち台帳に記録する者。
      CommercialPaper.Stateが実装しているContractStateインターフェースに
      由来し、CommercialPaper.Stateにおいては現所有者のみが該当

Commands
^^^^^^^^

.. class:: CommercialPaper.Issue
   :noindex:

   CPの発行を指示するcommand class

   .. attribute:: Input states

      - None

   .. attribute:: Output states

      - CommercialPaper.State(owner = issuer自身)

   .. attribute:: Required signers

      - 当該CommercialPaper.Stateのissuer

.. class:: Move
   :noindex:

   CPの所有権移転を指示するcommand class

   .. attribute:: Input states

      - CommercialPaper.State(owner = 移転前の所有者)

   .. attribute:: Output states

      - CommercialPaper.State(owner = 移転後の所有者)

   .. attribute:: Required signers

      - Input側のCommercialPaper.Stateのowner

   .. attribute:: Contract rules

      - owner以外の要素(faceValue等)に変更が無いこと

.. class:: Redeem
   :noindex:

   CPの償還を指示するcommand class

   .. attribute:: Input states

      - CommercialPaper.State(faceValue = CP額面, issuer = CP発行者, owner = CP現所有者)
      - [複数可] Cash.State(owner = CP発行者, amount = CP額面)

   .. attribute:: Output states

      - [複数可] Cash.State(owner = CP現所有者, amount = CP額面)

   .. attribute:: Required signers

      - 当該CommercialPaper.Stateのowner (CommercialPaper.Stateの消滅を許可するため)
      - 当該CommercialPaper.Stateのissuer (Cash.Stateの移転を許可するため)

   .. attribute:: Contract rules

      - 当該CommercialPaperがOutputに含まれないこと(消滅すること)
      - CP額面と同額分のCash.StateがCP発行者からCP現所有者へ(CPと引き換えに)Cash.Moveされること
