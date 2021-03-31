use super::client;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateClients {
        #[structopt(long, default_value = "http://localhost:9999")]
        endpoint_a: String,

        #[structopt(long, default_value = "http://localhost:19999")]
        endpoint_b: String,

        #[structopt(long)]
        client_id_a: String,

        #[structopt(long)]
        client_id_b: String,
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
        Opt::CreateClients {
            endpoint_a,
            endpoint_b,
            client_id_a,
            client_id_b,
        } => {
            client::create_clients(endpoint_a, endpoint_b, client_id_a, client_id_b).await?;
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
