use super::generated::ibc;
use super::Result;

use ibc::lightclients::corda::v1 as ibc_corda;

pub async fn shutdown(endpoint: String) -> Result<()> {
    let mut client = ibc_corda::admin_service_client::AdminServiceClient::connect(endpoint).await?;
    let _: tonic::Response<()> = client.shutdown(()).await?;
    Ok(())
}
