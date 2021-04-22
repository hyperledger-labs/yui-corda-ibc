use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

async fn connect(
    endpoint: String,
) -> Result<v1corda::bank_service_client::BankServiceClient<tonic::transport::Channel>> {
    let client = v1corda::bank_service_client::BankServiceClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn create_bank(endpoint: String) -> Result<()> {
    let mut client = connect(endpoint).await?;
    client.create_bank(()).await?;
    Ok(())
}

pub async fn query_bank(endpoint: String) -> Result<v1corda::Bank> {
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
        .allocate_fund(v1corda::AllocateFundRequest {
            owner: party_name,
            denom,
            amount,
        })
        .await?;
    Ok(())
}
