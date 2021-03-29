mod admin;
mod client;
mod generated;
mod genesis;
mod host_and_bank;
mod util;

use structopt::StructOpt;

type Result<T> = std::result::Result<T, Box<dyn std::error::Error>>;

#[derive(StructOpt, Debug)]
#[structopt(name = "corda-ibc-client")]
enum Opt {
    Shutdown {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
    CreateGenesis {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long, default_value = "PartyA")]
        party_name: String,
    },
    CreateHostAndBank {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
    QueryHost {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
    QueryBank {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
    AllocateFund {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long, default_value = "PartyA")]
        party_name: String,

        #[structopt(short, long, default_value = "USD")]
        denom: String,

        #[structopt(short, long)]
        amount: String,
    },
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
}

#[tokio::main]
async fn main() -> Result<()> {
    match Opt::from_args() {
        Opt::Shutdown { endpoint } => admin::shutdown(&endpoint).await?,
        Opt::CreateGenesis {
            endpoint,
            party_name,
        } => genesis::create_genesis(&endpoint, &party_name).await?,
        Opt::CreateHostAndBank { endpoint } => {
            host_and_bank::create_host_and_bank(&endpoint).await?
        }
        Opt::QueryHost { endpoint } => {
            let host = host_and_bank::query_host(&endpoint).await?;
            println!("{:?}", host);
        }
        Opt::QueryBank { endpoint } => {
            let bank = host_and_bank::query_bank(&endpoint).await?;
            println!("{:?}", bank);
        }
        Opt::AllocateFund {
            endpoint,
            party_name,
            denom,
            amount,
        } => host_and_bank::allocate_fund(&endpoint, &party_name, &denom, &amount).await?,
        Opt::CreateCordaClient {
            endpoint,
            client_id,
            counterparty_base_hash,
            counterparty_notary_key,
        } => {
            client::create_corda_client(
                &endpoint,
                &client_id,
                &counterparty_base_hash,
                &counterparty_notary_key,
            )
            .await?;
        }
    }

    Ok(())
}
