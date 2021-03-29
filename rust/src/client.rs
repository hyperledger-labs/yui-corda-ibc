use super::generated::ibc;
use super::util;
use super::Result;

use ibc::core::client::v1 as ibc_client;
use ibc::lightclients::corda::v1 as ibc_corda;

use prost_types::Any;

async fn connect_tx(
    endpoint: &str,
) -> Result<ibc_client::msg_client::MsgClient<tonic::transport::Channel>> {
    let client = ibc_client::msg_client::MsgClient::connect(endpoint.to_owned()).await?;
    Ok(client)
}

async fn connect_query(
    endpoint: &str,
) -> Result<ibc_client::query_client::QueryClient<tonic::transport::Channel>> {
    let client = ibc_client::query_client::QueryClient::connect(endpoint.to_owned()).await?;
    Ok(client)
}

pub async fn create_client(
    endpoint: &str,
    client_id: &str,
    client_state: Any,
    consensus_state: Any,
) -> Result<()> {
    let mut client = connect_tx(endpoint).await?;
    client
        .create_client(ibc_client::MsgCreateClient {
            client_id: client_id.to_owned(),
            client_state: Some(client_state),
            consensus_state: Some(consensus_state),
            signer: String::default(),
        })
        .await?;
    Ok(())
}

pub async fn create_corda_client(
    endpoint: &str,
    client_id: &str,
    counterparty_base_hash: &str,
    counterparty_notary_key: &str,
) -> Result<()> {
    let client_state = util::pack_any(
        "/ibc.lightclients.corda.v1.ClientState",
        &ibc_corda::ClientState {
            id: client_id.to_owned(),
        },
    )?;

    let consensus_state = util::pack_any(
        "/ibc.lightclients.corda.v1.ConsensusState",
        &ibc_corda::ConsensusState {
            base_id: Some(util::hex_to_base_id(counterparty_base_hash)?),
            notary_key: Some(util::hex_to_public_key(counterparty_notary_key)?),
        },
    )?;

    create_client(endpoint, client_id, client_state, consensus_state).await
}

pub async fn query_client_state(
    endpoint: &str,
    client_id: &str,
) -> Result<ibc_client::QueryClientStateResponse> {
    let mut client = connect_query(endpoint).await?;
    let response = client
        .client_state(ibc_client::QueryClientStateRequest {
            client_id: client_id.to_owned(),
        })
        .await?;
    Ok(response.into_inner())
}

pub async fn query_consensus_state(
    endpoint: &str,
    client_id: &str,
    version_number: u64,
    version_height: u64,
    latest_height: bool,
) -> Result<ibc_client::QueryConsensusStateResponse> {
    let mut client = connect_query(endpoint).await?;
    let response = client
        .consensus_state(ibc_client::QueryConsensusStateRequest {
            client_id: client_id.to_owned(),
            version_number,
            version_height,
            latest_height,
        })
        .await?;
    Ok(response.into_inner())
}
