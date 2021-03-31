use super::generated::ibc;
use super::Result;
use ibc::lightclients::corda::v1 as v1corda;

pub async fn create_genesis(endpoint: String, party_name: String) -> Result<()> {
    let participants = {
        let mut client =
            v1corda::node_service_client::NodeServiceClient::connect(endpoint.clone()).await?;
        let mut participants = vec![];
        for name in vec![String::from("Notary"), party_name] {
            let mut parties = client
                .parties_from_name(v1corda::PartiesFromNameRequest {
                    name,
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
            v1corda::genesis_service_client::GenesisServiceClient::connect(endpoint).await?;
        client
            .create_genesis(v1corda::CreateGenesisRequest { participants })
            .await?
            .into_inner()
            .base_id
    };

    let bash_hash = hex::encode(base_id.unwrap().txhash.unwrap().bytes);
    println!("{}", bash_hash);

    Ok(())
}
