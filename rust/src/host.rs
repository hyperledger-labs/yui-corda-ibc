use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;
use std::collections::HashMap;

async fn connect(
    endpoint: String,
) -> Result<v1corda::host_service_client::HostServiceClient<tonic::transport::Channel>> {
    let client = v1corda::host_service_client::HostServiceClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn create_host(
    endpoint: String,
    base_id_hash: String,
    module_names: HashMap<String, String>,
    client_state_factory_names: HashMap<String, String>,
) -> Result<()> {
    let mut client = connect(endpoint).await?;
    let base_id = v1corda::StateRef {
        txhash: Some(v1corda::SecureHash {
            bytes: hex::decode(base_id_hash).unwrap(),
        }),
        index: 0,
    };
    client
        .create_host(v1corda::CreateHostRequest {
            base_id: Some(base_id),
            module_names,
            client_state_factory_names,
        })
        .await?;
    Ok(())
}

pub async fn query_host(endpoint: String) -> Result<v1corda::Host> {
    let mut client = connect(endpoint).await?;
    let host = client
        .query_host(v1corda::QueryHostRequest { base_id: None })
        .await?;
    Ok(host.into_inner().host.unwrap())
}
