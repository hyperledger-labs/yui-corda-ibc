use super::constants::{CLIENT_STATE_TYPE_URL, CONSENSUS_STATE_TYPE_URL};
use super::generated::ibc;
use super::host;
use super::util;
use super::Result;
use ibc::core::client::v1 as v1client;
use ibc::lightclients::corda::v1 as v1corda;

async fn msg_client(
    endpoint: String,
) -> Result<v1corda::client_msg_client::ClientMsgClient<tonic::transport::Channel>> {
    Ok(v1corda::client_msg_client::ClientMsgClient::connect(endpoint).await?)
}

async fn query_client(
    endpoint: String,
) -> Result<v1corda::client_query_client::ClientQueryClient<tonic::transport::Channel>> {
    Ok(v1corda::client_query_client::ClientQueryClient::connect(endpoint).await?)
}

pub async fn create_clients(endpoint_a: String, endpoint_b: String) -> Result<()> {
    let host_a = host::query_host(endpoint_a.clone()).await?;
    let host_b = host::query_host(endpoint_b.clone()).await?;

    let client_state_a = v1corda::ClientState {
        base_id: host_b.base_id,
        notary_key: host_b.notary.and_then(|n| n.owning_key),
    };
    let client_state_a = util::pack_any(CLIENT_STATE_TYPE_URL.to_owned(), &client_state_a)?;

    let client_state_b = v1corda::ClientState {
        base_id: host_a.base_id,
        notary_key: host_a.notary.and_then(|n| n.owning_key),
    };
    let client_state_b = util::pack_any(CLIENT_STATE_TYPE_URL.to_owned(), &client_state_b)?;

    let consensus_state_a = util::pack_any(
        CONSENSUS_STATE_TYPE_URL.to_owned(),
        &v1corda::ConsensusState {},
    )?;
    let consensus_state_b = util::pack_any(
        CONSENSUS_STATE_TYPE_URL.to_owned(),
        &v1corda::ConsensusState {},
    )?;

    let mut client_a = msg_client(endpoint_a).await?;
    let mut client_b = msg_client(endpoint_b).await?;

    client_a
        .create_client(v1corda::CreateClientRequest {
            base_id: None,
            request: Some(v1client::MsgCreateClient {
                client_state: Some(client_state_a),
                consensus_state: Some(consensus_state_a),
                signer: Default::default(),
            }),
        })
        .await?;
    client_b
        .create_client(v1corda::CreateClientRequest {
            base_id: None,
            request: Some(v1client::MsgCreateClient {
                client_state: Some(client_state_b),
                consensus_state: Some(consensus_state_b),
                signer: Default::default(),
            }),
        })
        .await?;

    Ok(())
}

pub async fn query_client_state(
    endpoint: String,
    client_id: String,
) -> Result<v1client::QueryClientStateResponse> {
    let mut client = query_client(endpoint).await?;
    let response = client
        .client_state(v1corda::QueryClientStateRequest {
            base_id: None,
            request: Some(v1client::QueryClientStateRequest { client_id }),
        })
        .await?;
    Ok(response.into_inner().response.unwrap())
}

pub async fn query_consensus_state(
    endpoint: String,
    client_id: String,
    revision_number: u64,
    revision_height: u64,
    latest_height: bool,
) -> Result<v1client::QueryConsensusStateResponse> {
    let mut client = query_client(endpoint).await?;
    let response = client
        .consensus_state(v1corda::QueryConsensusStateRequest {
            base_id: None,
            request: Some(v1client::QueryConsensusStateRequest {
                client_id,
                revision_number,
                revision_height,
                latest_height,
            }),
        })
        .await?;
    Ok(response.into_inner().response.unwrap())
}
