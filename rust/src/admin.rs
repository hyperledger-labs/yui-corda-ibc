use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

pub async fn shutdown(endpoint: String) -> Result<()> {
    let mut client = v1corda::admin_service_client::AdminServiceClient::connect(endpoint).await?;
    let _: tonic::Response<()> = client.shutdown(()).await?;
    Ok(())
}
