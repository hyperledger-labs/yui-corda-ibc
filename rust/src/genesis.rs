use super::generated::ibc;
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
            ibc_corda::genesis_service_client::GenesisServiceClient::connect(endpoint.to_owned())
                .await?;
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
