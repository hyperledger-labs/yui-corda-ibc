use super::generated::ibc;
use super::node;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

async fn connect(
    endpoint: String,
) -> Result<v1corda::genesis_service_client::GenesisServiceClient<tonic::transport::Channel>> {
    let client = v1corda::genesis_service_client::GenesisServiceClient::connect(endpoint).await?;
    Ok(client)
}

pub async fn create_genesis(endpoint: String, party_names: String) -> Result<()> {
    let party_names: Vec<&str> = party_names.split(',').collect();
    let mut participants = Vec::with_capacity(party_names.len());
    for name in party_names.into_iter() {
        eprintln!("get Party of {}", name);
        let party = node::party_from_name(endpoint.clone(), name.to_string()).await?;
        participants.push(party);
    }

    let mut client = connect(endpoint).await?;
    let base_id = client
        .create_genesis(v1corda::CreateGenesisRequest { participants })
        .await?
        .into_inner()
        .base_id;

    let bash_hash = hex::encode(base_id.unwrap().txhash.unwrap().bytes);
    println!("{}", bash_hash);

    Ok(())
}
