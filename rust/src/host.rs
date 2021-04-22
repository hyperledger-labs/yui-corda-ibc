use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

async fn connect(
    endpoint: String,
) -> Result<v1corda::host_service_client::HostServiceClient<tonic::transport::Channel>> {
    let client = v1corda::host_service_client::HostServiceClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn create_host(endpoint: String) -> Result<()> {
    let mut client = connect(endpoint).await?;
    client.create_host(()).await?;
    Ok(())
}

pub async fn query_host(endpoint: String) -> Result<v1corda::Host> {
    let mut client = connect(endpoint).await?;
    let host = client.query_host(()).await?;
    Ok(host.into_inner())
}
