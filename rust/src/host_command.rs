use super::host;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateHost {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
    QueryHost {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateHost { endpoint } => host::create_host(endpoint).await?,
        Opt::QueryHost { endpoint } => {
            let host = host::query_host(endpoint).await?;
            println!("{:?}", host);
        }
    }
    Ok(())
}
