package jp.datachain.corda.ibc.cosmos

import jp.datachain.cosmos.types.rest.BaseReq
import jp.datachain.cosmos.x.auth.types.BaseAccount

class CosmosTransactor(val from: String, val chainID: String, val accountNumber: Int, var sequence: Int) {
    constructor(account: BaseAccount, chainID: String)
            :this(account.address, chainID, account.accountNumber ?: 0, account.sequence ?: 0)

    inline operator fun <reified REQ: CosmosRequest> invoke(req: REQ) : REQ {
        req.baseReq = BaseReq(
                from = from,
                chainID = chainID,
                accountNumber = accountNumber.toString(),
                sequence = sequence.toString())
        sequence += 1
        return req
    }
}