package jp.datachain.corda.ibc.cosmos

import jp.datachain.amino.DisfixWrapper
import jp.datachain.cosmos.types.TxResponse
import jp.datachain.cosmos.types.rest.ResponseWithHeight
import jp.datachain.cosmos.x.auth.client.rest.BroadcastReq
import jp.datachain.cosmos.x.auth.types.StdTx
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import kotlin.reflect.full.findAnnotation

class CosmosRESTClient(val host: String, val port: Int) {
    val client = JavaHttpClient()

    fun query(path: String) : ResponseWithHeight {
        val lens = Body.auto<ResponseWithHeight>().toLens()
        val request = Request(Method.GET, "http://${host}:${port}/${path}")
        return lens(client(request))
    }

    inline fun <reified REQ: CosmosRequest> request(req: REQ, vararg pathArgs: String) : DisfixWrapper {
        val setLens = Body.auto<REQ>().toLens()
        val getLens = Body.auto<DisfixWrapper>().toLens()
        val path = REQ::class.findAnnotation<ReqPath>()!!.path(*pathArgs)
        val request = Request(Method.POST, "http://$host:$port/$path")
        val response = client(setLens(req, request))
        if (response.status.successful) {
            return getLens(response)
        } else {
            throw RuntimeException(response.toString())
        }
    }

    fun broadcast(tx: StdTx, mode: String) : TxResponse {
        val setLens = Body.auto<BroadcastReq>().toLens()
        val getLens = Body.auto<TxResponse>().toLens()
        val request = Request(Method.POST, "http://$host:$port/txs")
        return getLens(client(setLens(BroadcastReq(tx, mode), request)))
    }
}