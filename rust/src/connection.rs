use super::generated::ibc;
use super::util;
use super::Result;
use ibc::core::connection::v1 as v1connection;

async fn connect_tx(
    endpoint: String,
) -> Result<v1connection::msg_client::MsgClient<tonic::transport::Channel>> {
    let client = v1connection::msg_client::MsgClient::connect(endpoint).await?;
    Ok(client)
}

async fn connect_query(
    endpoint: String,
) -> Result<v1connection::query_client::QueryClient<tonic::transport::Channel>> {
    let client = v1connection::query_client::QueryClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn conn_open_init(
    endpoint: String,
    client_id: String,
    connection_id: String,
    counterparty_client_id: String,
    counterparty_connection_id: String,
    counterparty_prefix: String,
    version_identifier: String,
    version_features: Vec<String>,
) -> Result<()> {
    let mut client = connect_tx(endpoint).await?;
    client
        .connection_open_init(v1connection::MsgConnectionOpenInit {
            client_id,
            connection_id,
            counterparty: Some(v1connection::Counterparty {
                client_id: counterparty_client_id,
                connection_id: counterparty_connection_id,
                prefix: Some(util::hex_to_prefix(&counterparty_prefix)?),
            }),
            version: Some(v1connection::Version {
                identifier: version_identifier,
                features: version_features,
            }),
            signer: Default::default(),
        })
        .await?;
    Ok(())
}

pub async fn query_connection(
    endpoint: String,
    connection_id: String,
) -> Result<v1connection::QueryConnectionResponse> {
    let mut client = connect_query(endpoint).await?;
    let response = client
        .connection(v1connection::QueryConnectionRequest { connection_id })
        .await?;
    Ok(response.into_inner())
}
