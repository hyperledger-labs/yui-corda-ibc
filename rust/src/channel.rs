use super::constants::CHANNEL_VERSION;
use super::generated::ibc;
use super::Result;
use ibc::core::channel::v1 as v1channel;

async fn msg_client(
    endpoint: String,
) -> Result<v1channel::msg_client::MsgClient<tonic::transport::Channel>> {
    let client = v1channel::msg_client::MsgClient::connect(endpoint).await?;
    Ok(client)
}

async fn query_client(
    endpoint: String,
) -> Result<v1channel::query_client::QueryClient<tonic::transport::Channel>> {
    let client = v1channel::query_client::QueryClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn handshake(
    endpoint_a: String,
    endpoint_b: String,
    connection_id_a: String,
    connection_id_b: String,
    port_id_a: String,
    port_id_b: String,
    channel_id_a: String,
    channel_id_b: String,
) -> Result<()> {
    let mut client_a = msg_client(endpoint_a.clone()).await?;
    let mut client_b = msg_client(endpoint_b.clone()).await?;

    client_a
        .channel_open_init(v1channel::MsgChannelOpenInit {
            port_id: port_id_a.clone(),
            channel: Some(v1channel::Channel {
                state: v1channel::State::UninitializedUnspecified as i32,
                ordering: v1channel::Order::Ordered as i32,
                counterparty: Some(v1channel::Counterparty {
                    port_id: port_id_b.clone(),
                    channel_id: channel_id_b.clone(),
                }),
                connection_hops: vec![connection_id_a],
                version: CHANNEL_VERSION.to_owned(),
            }),
            signer: Default::default(),
        })
        .await?;

    {
        let counterparty_channel =
            query_channel(endpoint_a.clone(), port_id_a.clone(), channel_id_a.clone()).await?;

        client_b
            .channel_open_try(v1channel::MsgChannelOpenTry {
                port_id: port_id_b.clone(),
                previous_channel_id: "".to_owned(),
                channel: Some(v1channel::Channel {
                    state: v1channel::State::Tryopen as i32,
                    ordering: v1channel::Order::Ordered as i32,
                    counterparty: Some(v1channel::Counterparty {
                        port_id: port_id_a.clone(),
                        channel_id: channel_id_a.clone(),
                    }),
                    connection_hops: vec![connection_id_b],
                    version: CHANNEL_VERSION.to_owned(),
                }),
                counterparty_version: CHANNEL_VERSION.to_owned(),
                proof_init: counterparty_channel.proof,
                proof_height: counterparty_channel.proof_height,
                signer: Default::default(),
            })
            .await?;
    }

    {
        let counterparty_channel =
            query_channel(endpoint_b, port_id_b.clone(), channel_id_b.clone()).await?;

        client_a
            .channel_open_ack(v1channel::MsgChannelOpenAck {
                port_id: port_id_a.clone(),
                channel_id: channel_id_a.clone(),
                counterparty_channel_id: channel_id_b.clone(),
                counterparty_version: CHANNEL_VERSION.to_owned(),
                proof_try: counterparty_channel.proof,
                proof_height: counterparty_channel.proof_height,
                signer: Default::default(),
            })
            .await?;
    }

    {
        let counterparty_channel = query_channel(endpoint_a, port_id_a, channel_id_a).await?;

        client_b
            .channel_open_confirm(v1channel::MsgChannelOpenConfirm {
                port_id: port_id_b,
                channel_id: channel_id_b,
                proof_ack: counterparty_channel.proof,
                proof_height: counterparty_channel.proof_height,
                signer: Default::default(),
            })
            .await?;
    }

    Ok(())
}

pub async fn query_channel(
    endpoint: String,
    port_id: String,
    channel_id: String,
) -> Result<v1channel::QueryChannelResponse> {
    let mut client = query_client(endpoint).await?;
    let response = client
        .channel(v1channel::QueryChannelRequest {
            port_id,
            channel_id,
        })
        .await?;
    Ok(response.into_inner())
}
