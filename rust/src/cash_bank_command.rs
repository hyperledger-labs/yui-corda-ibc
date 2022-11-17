use super::cash_bank;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateCashBank {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        bank_address: String,
    },
    AllocateCash {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        owner_address: String,

        #[structopt(short, long)]
        currency: String,

        #[structopt(short, long)]
        amount: String,
    },
    QueryCashBank {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateCashBank {
            endpoint,
            bank_address,
        } => cash_bank::create_cash_bank(endpoint, bank_address).await?,
        Opt::AllocateCash {
            endpoint,
            owner_address,
            currency,
            amount,
        } => cash_bank::allocate_cash(endpoint, owner_address, currency, amount).await?,
        Opt::QueryCashBank { endpoint } => {
            let host = cash_bank::query_cash_bank(endpoint).await?;
            println!("{:#?}", host);
        }
    }
    Ok(())
}
