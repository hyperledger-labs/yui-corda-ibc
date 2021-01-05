package jp.datachain.corda.ibc.grpc_adapter

import ibc.core.client.v1.Client
import ibc.core.client.v1.MsgGrpc
import io.grpc.stub.StreamObserver
import jp.datachain.corda.ibc.flows.IbcClientCreateFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateRef
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort

class IbcClientService(host: String, port: Int, username: String, password: String, private val baseId: StateRef): MsgGrpc.MsgImplBase() {
    private val ops = CordaRPCClient(NetworkHostAndPort(host, port))
            .start(username, password)
            .proxy

    override fun createClient(request: Client.MsgCreateClient, responseObserver: StreamObserver<Client.MsgCreateClientResponse>) {
        ops.startFlow(::IbcClientCreateFlow, baseId, request).returnValue.get()
        responseObserver.onCompleted()
    }
}