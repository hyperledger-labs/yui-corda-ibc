use super::host;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateHost {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        base_id_hash: String,
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
        } => host::create_host(endpoint, base_id_hash).await?,
        Opt::QueryHost { endpoint } => {
            let host = host::query_host(endpoint).await?;
            println!("{:#?}", host);
        }
    }
    Ok(())
}
