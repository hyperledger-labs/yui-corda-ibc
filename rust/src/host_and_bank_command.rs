use super::host_and_bank;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
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
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateHostAndBank { endpoint } => {
            host_and_bank::create_host_and_bank(endpoint).await?
        }
        Opt::QueryHost { endpoint } => {
            let host = host_and_bank::query_host(endpoint).await?;
            println!("{:?}", host);
        }
        Opt::QueryBank { endpoint } => {
            let bank = host_and_bank::query_bank(endpoint).await?;
            println!("{:?}", bank);
        }
        Opt::AllocateFund {
            endpoint,
            party_name,
            denom,
            amount,
        } => host_and_bank::allocate_fund(endpoint, party_name, denom, amount).await?,
    }
    Ok(())
}
