use super::client;
use super::constants::{DELAY_PERIOD, HEIGHT, PREFIX, VERSION};
use super::generated::ibc;
use super::Result;
use ibc::core::connection::v1 as v1connection;

async fn msg_client(
    endpoint: String,
) -> Result<v1connection::msg_client::MsgClient<tonic::transport::Channel>> {
    let client = v1connection::msg_client::MsgClient::connect(endpoint).await?;
    Ok(client)
}

async fn query_client(
    endpoint: String,
) -> Result<v1connection::query_client::QueryClient<tonic::transport::Channel>> {
    let client = v1connection::query_client::QueryClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn handshake(
    endpoint_a: String,
    endpoint_b: String,
    client_id_a: String,
    client_id_b: String,
    connection_id_a: String,
    connection_id_b: String,
) -> Result<()> {
    let mut client_a = msg_client(endpoint_a.clone()).await?;
    let mut client_b = msg_client(endpoint_b.clone()).await?;

    client_a
        .connection_open_init(v1connection::MsgConnectionOpenInit {
            client_id: client_id_a.clone(),
            counterparty: Some(v1connection::Counterparty {
                client_id: client_id_b.clone(),
                connection_id: "".to_owned(),
                prefix: Some(PREFIX.clone()),
            }),
            version: Some(VERSION.clone()),
            delay_period: DELAY_PERIOD,
            signer: Default::default(),
        })
        .await?;

    {
        let counterparty_client_state =
            client::query_client_state(endpoint_a.clone(), client_id_a.clone()).await?;
        let counterparty_consensus_state = client::query_consensus_state(
            endpoint_a.clone(),
            client_id_a.clone(),
            HEIGHT.revision_number,
            HEIGHT.revision_height,
            false,
        )
        .await?;
        let counterparty_connection =
            query_connection(endpoint_a.clone(), connection_id_a.clone()).await?;

        client_b
            .connection_open_try(v1connection::MsgConnectionOpenTry {
                client_id: client_id_b.clone(),
                previous_connection_id: "".to_owned(),
                client_state: counterparty_client_state.client_state,
                counterparty: Some(v1connection::Counterparty {
                    client_id: client_id_a,
                    connection_id: connection_id_a.clone(),
                    prefix: Some(PREFIX.clone()),
                }),
                counterparty_versions: vec![VERSION.clone()],
                proof_height: counterparty_connection.proof_height,
                proof_init: counterparty_connection.proof,
                proof_client: counterparty_client_state.proof,
                proof_consensus: counterparty_consensus_state.proof,
                consensus_height: Some(HEIGHT.clone()),
                delay_period: DELAY_PERIOD,
                signer: Default::default(),
            })
            .await?;
    }

    {
        let counterparty_client_state =
            client::query_client_state(endpoint_b.clone(), client_id_b.clone()).await?;
        let counterparty_consensus_state = client::query_consensus_state(
            endpoint_b.clone(),
            client_id_b,
            HEIGHT.revision_number,
            HEIGHT.revision_height,
            false,
        )
        .await?;
        let counterparty_connection = query_connection(endpoint_b, connection_id_b.clone()).await?;

        client_a
            .connection_open_ack(v1connection::MsgConnectionOpenAck {
                connection_id: connection_id_a.clone(),
                counterparty_connection_id: connection_id_b.clone(),
                version: Some(VERSION.clone()),
                client_state: counterparty_client_state.client_state,
                proof_height: counterparty_connection.proof_height,
                proof_try: counterparty_connection.proof,
                proof_client: counterparty_client_state.proof,
                proof_consensus: counterparty_consensus_state.proof,
                consensus_height: Some(HEIGHT.clone()),
                signer: Default::default(),
            })
            .await?;
    }

    {
        let counterparty_connection = query_connection(endpoint_a, connection_id_a).await?;

        client_b
            .connection_open_confirm(v1connection::MsgConnectionOpenConfirm {
                connection_id: connection_id_b,
                proof_ack: counterparty_connection.proof,
                proof_height: counterparty_connection.proof_height,
                signer: Default::default(),
            })
            .await?;
    }

    Ok(())
}

pub async fn query_connection(
    endpoint: String,
    connection_id: String,
) -> Result<v1connection::QueryConnectionResponse> {
    let mut client = query_client(endpoint).await?;
    let response = client
        .connection(v1connection::QueryConnectionRequest { connection_id })
        .await?;
    Ok(response.into_inner())
}
