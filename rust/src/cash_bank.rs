use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

async fn connect(
    endpoint: String,
) -> Result<v1corda::cash_bank_service_client::CashBankServiceClient<tonic::transport::Channel>> {
    let client =
        v1corda::cash_bank_service_client::CashBankServiceClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn create_cash_bank(endpoint: String, bank_address: String) -> Result<()> {
    let mut client = connect(endpoint).await?;
    client
        .create_cash_bank(v1corda::CreateCashBankRequest {
            base_id: None,
            bank_address,
        })
        .await?;
    Ok(())
}

pub async fn query_cash_bank(endpoint: String) -> Result<v1corda::CashBank> {
    let mut client = connect(endpoint).await?;
    let bank = client
        .query_cash_bank(v1corda::QueryCashBankRequest { base_id: None })
        .await?;
    Ok(bank.into_inner().cash_bank.unwrap())
}

pub async fn allocate_cash(
    endpoint: String,
    owner_address: String,
    currency: String,
    amount: String,
) -> Result<()> {
    let mut client = connect(endpoint).await?;
    client
        .allocate_cash(v1corda::AllocateCashRequest {
            base_id: None,
            owner_address,
            currency,
            amount,
        })
        .await?;
    Ok(())
}
