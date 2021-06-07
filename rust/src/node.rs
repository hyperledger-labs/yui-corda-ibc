use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

async fn connect(
    endpoint: String,
) -> Result<v1corda::node_service_client::NodeServiceClient<tonic::transport::Channel>> {
    let client = v1corda::node_service_client::NodeServiceClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn party_from_name(endpoint: String, name: String) -> Result<v1corda::Party> {
    let mut client = connect(endpoint).await?;
    let response = client
        .party_from_name(v1corda::PartyFromNameRequest {
            name,
            exact_match: false,
        })
        .await?;
    Ok(response.into_inner().party.unwrap())
}

pub async fn address_from_name(endpoint: String, name: String) -> Result<String> {
    let mut client = connect(endpoint).await?;
    let response = client
        .address_from_name(v1corda::AddressFromNameRequest {
            name,
            exact_match: false,
        })
        .await?;
    Ok(response.into_inner().address)
}
