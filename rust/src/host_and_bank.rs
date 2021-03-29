use super::generated::ibc;
use super::util;
use super::Result;

use ibc::lightclients::corda::v1 as ibc_corda;

pub async fn create_genesis(endpoint: &str, party_name: &str) -> Result<()> {
    let participants = {
        let mut client =
            ibc_corda::node_service_client::NodeServiceClient::connect(endpoint.to_owned()).await?;
        let mut participants = vec![];
        for &name in ["Notary", party_name].iter() {
            let mut parties = client
                .parties_from_name(ibc_corda::PartiesFromNameRequest {
                    name: name.to_owned(),
                    exact_match: false,
                })
                .await?
                .into_inner()
                .parties;
            assert!(parties.len() == 1);
            participants.push(parties.remove(0));
        }
        participants
    };

    let base_id = {
        let mut client =
            ibc_corda::ibc_service_client::IbcServiceClient::connect(endpoint.to_owned()).await?;
        client
            .create_genesis(ibc_corda::CreateGenesisRequest { participants })
            .await?
            .into_inner()
            .base_id
    };

    let bash_hash = hex::encode(base_id.unwrap().txhash.unwrap().bytes);
    println!("{}", bash_hash);

    Ok(())
}

pub async fn create_host_and_bank(endpoint: &str, base_hash: &str) -> Result<()> {
    let mut client =
        ibc_corda::ibc_service_client::IbcServiceClient::connect(endpoint.to_owned()).await?;
    let base_id = util::hex_to_base_id(base_hash)?;
    client
        .create_host_and_bank(ibc_corda::CreateHostAndBankRequest {
            base_id: Some(base_id),
        })
        .await?;
    Ok(())
}

pub async fn query_host(endpoint: &str) -> Result<ibc_corda::Host> {
    let mut client =
        ibc_corda::query_service_client::QueryServiceClient::connect(endpoint.to_owned()).await?;
    let host = client.query_host(()).await?;
    Ok(host.into_inner())
}

pub async fn query_bank(endpoint: &str) -> Result<ibc_corda::Bank> {
    let mut client =
        ibc_corda::query_service_client::QueryServiceClient::connect(endpoint.to_owned()).await?;
    let bank = client.query_bank(()).await?;
    Ok(bank.into_inner())
}

pub async fn allocate_fund(
    endpoint: &str,
    base_hash: &str,
    party_name: &str,
    denom: &str,
    amount: &str,
) -> Result<()> {
    let mut client =
        ibc_corda::ibc_service_client::IbcServiceClient::connect(endpoint.to_owned()).await?;
    let base_id = util::hex_to_base_id(base_hash)?;
    client
        .allocate_fund(ibc_corda::AllocateFundRequest {
            base_id: Some(base_id),
            owner: party_name.to_owned(),
            denom: denom.to_owned(),
            amount: amount.to_owned(),
        })
        .await?;
    Ok(())
}
