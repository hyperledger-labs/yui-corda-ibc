use super::genesis;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateGenesis {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long, default_value = "PartyA")]
        party_name: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateGenesis {
            endpoint,
            party_name,
        } => genesis::create_genesis(&endpoint, &party_name).await?,
    }
    Ok(())
}
