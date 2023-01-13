use super::host;
use super::util;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateHost {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        base_id_hash: String,

        #[structopt(short, long)]
        module_names: Vec<String>,
    },
    QueryHost {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateHost {
            endpoint,
            base_id_hash,
            module_names,
        } => host::create_host(endpoint, base_id_hash, util::vec_to_map(&module_names)).await?,
        Opt::QueryHost { endpoint } => {
            let host = host::query_host(endpoint).await?;
            println!("{:#?}", host);
        }
    }
    Ok(())
}
