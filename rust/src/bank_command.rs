use super::bank;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateBank {
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
        Opt::CreateBank { endpoint } => bank::create_bank(endpoint).await?,
        Opt::QueryBank { endpoint } => {
            let bank = bank::query_bank(endpoint).await?;
            println!("{:?}", bank);
        }
        Opt::AllocateFund {
            endpoint,
            party_name,
            denom,
            amount,
        } => bank::allocate_fund(endpoint, party_name, denom, amount).await?,
    }
    Ok(())
}
