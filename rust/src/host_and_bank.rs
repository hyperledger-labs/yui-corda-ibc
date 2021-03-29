use super::generated::ibc;
use super::Result;

use ibc::lightclients::corda::v1 as ibc_corda;

async fn connect(
    endpoint: String,
) -> Result<
    ibc_corda::host_and_bank_service_client::HostAndBankServiceClient<tonic::transport::Channel>,
> {
    let client =
        ibc_corda::host_and_bank_service_client::HostAndBankServiceClient::connect(endpoint)
            .await?;
    Ok(client)
}

pub async fn create_host_and_bank(endpoint: String) -> Result<()> {
    let mut client = connect(endpoint).await?;
    client.create_host_and_bank(()).await?;
    Ok(())
}

pub async fn query_host(endpoint: String) -> Result<ibc_corda::Host> {
    let mut client = connect(endpoint).await?;
    let host = client.query_host(()).await?;
    Ok(host.into_inner())
}

pub async fn query_bank(endpoint: String) -> Result<ibc_corda::Bank> {
    let mut client = connect(endpoint).await?;
    let bank = client.query_bank(()).await?;
    Ok(bank.into_inner())
}

pub async fn allocate_fund(
    endpoint: String,
    party_name: String,
    denom: String,
    amount: String,
) -> Result<()> {
    let mut client = connect(endpoint).await?;
    client
        .allocate_fund(ibc_corda::AllocateFundRequest {
            owner: party_name,
            denom,
            amount,
        })
        .await?;
    Ok(())
}
