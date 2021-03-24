mod generated;
use generated::*;

use bytes::BytesMut;
use prost::Message;
use prost_types::Any;

use ibc::core::client::v1::{msg_client, Height, MsgCreateClient};
use ibc::lightclients::corda::v1::{
    query_service_client, ClientState, ConsensusState, QueryHostRequest,
};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut height = Height::default();
    height.version_number = 0;
    height.version_height = 1;
    println!("height: {:?}", height);

    let mut query_client =
        query_service_client::QueryServiceClient::connect("http://localhost:9999").await?;

    let request = tonic::Request::new(QueryHostRequest {});
    let host = query_client.query_host(request).await?;
    let host = host.into_inner();
    println!("host: {:?}", host);

    let client_id = "ibczeroclient";

    let client_state = ClientState {
        id: client_id.to_owned(),
    };
    let mut buf = BytesMut::default();
    client_state.encode(&mut buf)?;
    let any_client_state = Any {
        type_url: "/ibc.lightclients.corda.v1.ClientState".to_owned(),
        value: buf.to_vec(),
    };

    let consensus_state = ConsensusState {
        base_id: host.base_id,
        notary_key: host.notary.unwrap().owning_key,
    };
    let mut buf = BytesMut::default();
    consensus_state.encode(&mut buf)?;
    let any_consensus_state = Any {
        type_url: "/ibc.lightclients.corda.v1.ConsensusState".to_owned(),
        value: buf.to_vec(),
    };

    let mut client_client = msg_client::MsgClient::connect("http://localhost:9999").await?;

    let request = tonic::Request::new(MsgCreateClient {
        client_id: client_id.to_owned(),
        client_state: Some(any_client_state),
        consensus_state: Some(any_consensus_state),
        signer: "".to_owned(),
    });

    let response = client_client.create_client(request).await?;

    println!("response: {:?}", response);

    Ok(())
}
