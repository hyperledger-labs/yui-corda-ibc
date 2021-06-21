use super::node;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    PartyFromName {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        name: String,
    },
    AddressFromName {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        name: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::PartyFromName { endpoint, name } => {
            let party = node::party_from_name(endpoint, name).await?;
            println!("{:?}", party);
        }
        Opt::AddressFromName { endpoint, name } => {
            let address = node::address_from_name(endpoint, name).await?;
            println!("{}", address);
        }
    }
    Ok(())
}
