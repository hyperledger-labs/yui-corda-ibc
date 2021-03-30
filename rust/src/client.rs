use super::generated::ibc;
use super::host_and_bank;
use super::util;
use super::Result;
use ibc::core::client::v1 as v1client;
use ibc::lightclients::corda::v1 as v1corda;

const CORDA_CLIENT_STATE_TYPE_URL: &str = "/ibc.lightclients.corda.v1.ClientState";
const CORDA_CONSENSUS_STATE_TYPE_URL: &str = "/ibc.lightclients.corda.v1.ConsensusState";

async fn msg_client(
    endpoint: String,
) -> Result<v1client::msg_client::MsgClient<tonic::transport::Channel>> {
    Ok(v1client::msg_client::MsgClient::connect(endpoint).await?)
}

async fn query_client(
    endpoint: String,
) -> Result<v1client::query_client::QueryClient<tonic::transport::Channel>> {
    Ok(v1client::query_client::QueryClient::connect(endpoint).await?)
}

pub async fn create_clients(
    endpoint_a: String,
    endpoint_b: String,
    client_id_a: String,
    client_id_b: String,
) -> Result<()> {
    let (client_id_a, client_state_a) = {
        let tmp = v1corda::ClientState { id: client_id_a };
        let packed = util::pack_any(CORDA_CLIENT_STATE_TYPE_URL.to_owned(), &tmp)?;
        (tmp.id, packed)
    };
    let (client_id_b, client_state_b) = {
        let tmp = v1corda::ClientState { id: client_id_b };
        let packed = util::pack_any(CORDA_CLIENT_STATE_TYPE_URL.to_owned(), &tmp)?;
        (tmp.id, packed)
    };

    let host_a = host_and_bank::query_host(endpoint_a.clone()).await?;
    let host_b = host_and_bank::query_host(endpoint_b.clone()).await?;

    let consensus_state_a = util::pack_any(
        CORDA_CONSENSUS_STATE_TYPE_URL.to_owned(),
        &v1corda::ConsensusState {
            base_id: host_b.base_id,
            notary_key: host_b.notary.and_then(|n| n.owning_key),
        },
    )?;
    let consensus_state_b = util::pack_any(
        CORDA_CONSENSUS_STATE_TYPE_URL.to_owned(),
        &v1corda::ConsensusState {
            base_id: host_a.base_id,
            notary_key: host_a.notary.and_then(|n| n.owning_key),
        },
    )?;

    let mut client_a = msg_client(endpoint_a).await?;
    let mut client_b = msg_client(endpoint_b).await?;

    client_a
        .create_client(v1client::MsgCreateClient {
            client_id: client_id_a,
            client_state: Some(client_state_a),
            consensus_state: Some(consensus_state_a),
            signer: Default::default(),
        })
        .await?;
    client_b
        .create_client(v1client::MsgCreateClient {
            client_id: client_id_b,
            client_state: Some(client_state_b),
            consensus_state: Some(consensus_state_b),
            signer: Default::default(),
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
        .client_state(v1client::QueryClientStateRequest { client_id })
        .await?;
    Ok(response.into_inner())
}

pub async fn query_consensus_state(
    endpoint: String,
    client_id: String,
    version_number: u64,
    version_height: u64,
    latest_height: bool,
) -> Result<v1client::QueryConsensusStateResponse> {
    let mut client = query_client(endpoint).await?;
    let response = client
        .consensus_state(v1client::QueryConsensusStateRequest {
            client_id,
            version_number,
            version_height,
            latest_height,
        })
        .await?;
    Ok(response.into_inner())
}
