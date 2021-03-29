mod generated;
use generated::ibc;

use prost_types::Any;

use ibc::core::client::v1 as ibc_client;
use ibc::lightclients::corda::v1 as ibc_corda;

type Result<T> = std::result::Result<T, Box<dyn std::error::Error>>;

fn hex_to_base_id(hex: &str) -> Result<ibc_corda::StateRef> {
    Ok(ibc_corda::StateRef {
        txhash: Some(ibc_corda::SecureHash {
            bytes: hex::decode(hex)?,
        }),
        index: 0,
    })
}

fn hex_to_public_key(hex: &str) -> Result<ibc_corda::PublicKey> {
    Ok(ibc_corda::PublicKey {
        encoded: hex::decode(hex)?,
    })
}

fn pack_any(type_url: &str, msg: &impl prost::Message) -> Result<Any> {
    let mut buf = Vec::default();
    msg.encode(&mut buf)?;
    Ok(Any {
        type_url: type_url.to_owned(),
        value: buf.into(),
    })
}

async fn shutdown(endpoint: &str) -> Result<()> {
    let mut client =
        ibc_corda::admin_service_client::AdminServiceClient::connect(endpoint.to_owned()).await?;
    let _: tonic::Response<()> = client.shutdown(()).await?;
    Ok(())
}

async fn create_genesis(endpoint: &str, party_name: &str) -> Result<()> {
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

async fn create_host(endpoint: &str, base_hash: &str) -> Result<()> {
    let mut client =
        ibc_corda::ibc_service_client::IbcServiceClient::connect(endpoint.to_owned()).await?;
    let base_id = hex_to_base_id(base_hash)?;
    client
        .create_host_and_bank(ibc_corda::CreateHostAndBankRequest {
            base_id: Some(base_id),
        })
        .await?;
    Ok(())
}

async fn query_host(endpoint: &str) -> Result<ibc_corda::Host> {
    let mut client =
        ibc_corda::query_service_client::QueryServiceClient::connect(endpoint.to_owned()).await?;
    let host = client.query_host(()).await?;
    Ok(host.into_inner())
}

async fn allocate_fund(
    endpoint: &str,
    base_hash: &str,
    party_name: &str,
    denom: &str,
    amount: &str,
) -> Result<()> {
    let mut client =
        ibc_corda::ibc_service_client::IbcServiceClient::connect(endpoint.to_owned()).await?;
    let base_id = hex_to_base_id(base_hash)?;
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

async fn create_corda_client(
    endpoint: &str,
    client_id: &str,
    counterparty_base_hash: &str,
    counterparty_notary_key: &str,
) -> Result<()> {
    let mut client = ibc_client::msg_client::MsgClient::connect(endpoint.to_owned()).await?;

    let client_state = pack_any(
        "/ibc.lightclients.corda.v1.ClientState",
        &ibc_corda::ClientState {
            id: client_id.to_owned(),
        },
    )?;

    let consensus_state = pack_any(
        "/ibc.lightclients.corda.v1.ConsensusState",
        &ibc_corda::ConsensusState {
            base_id: Some(hex_to_base_id(counterparty_base_hash)?),
            notary_key: Some(hex_to_public_key(counterparty_notary_key)?),
        },
    )?;

    client
        .create_client(ibc_client::MsgCreateClient {
            client_id: client_id.to_owned(),
            client_state: Some(client_state),
            consensus_state: Some(consensus_state),
            signer: String::default(),
        })
        .await?;
    Ok(())
}

#[tokio::main]
async fn main() -> Result<()> {
    let mut height = ibc_client::Height::default();
    height.version_number = 0;
    height.version_height = 1;
    println!("height: {:?}", height);

    let endpoint = "http://localhost:9999";

    let host = query_host(endpoint).await?;
    println!("host: {:?}", host);

    create_corda_client(
        "http://localhost:9999",
        "ibczeroclient",
        &hex::encode(host.base_id.unwrap().txhash.unwrap().bytes),
        &hex::encode(host.notary.unwrap().owning_key.unwrap().encoded),
    )
    .await?;

    Ok(())
}
