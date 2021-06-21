use super::genesis;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateGenesis {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        party_names: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateGenesis {
            endpoint,
            party_names,
        } => genesis::create_genesis(endpoint, party_names).await?,
    }
    Ok(())
}
