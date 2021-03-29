use super::client;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateCordaClient {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        client_id: String,

        #[structopt(short = "b", long)]
        counterparty_base_hash: String,

        #[structopt(short = "n", long)]
        counterparty_notary_key: String,
    },
    QueryClientState {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        client_id: String,
    },
    QueryConsensusState {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        client_id: String,

        #[structopt(short = "n", long)]
        version_number: u64,

        #[structopt(short = "h", long)]
        version_height: u64,

        #[structopt(short, long)]
        latest_height: bool,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateCordaClient {
            endpoint,
            client_id,
            counterparty_base_hash,
            counterparty_notary_key,
        } => {
            client::create_corda_client(
                endpoint,
                client_id,
                counterparty_base_hash,
                counterparty_notary_key,
            )
            .await?;
        }
        Opt::QueryClientState {
            endpoint,
            client_id,
        } => {
            let response = client::query_client_state(endpoint, client_id).await?;
            println!("{:?}", response);
        }
        Opt::QueryConsensusState {
            endpoint,
            client_id,
            version_number,
            version_height,
            latest_height,
        } => {
            let response = client::query_consensus_state(
                endpoint,
                client_id,
                version_number,
                version_height,
                latest_height,
            )
            .await?;
            println!("{:?}", response);
        }
    }
    Ok(())
}
