use super::admin;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    Shutdown {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::Shutdown { endpoint } => admin::shutdown(&endpoint).await?,
    }
    Ok(())
}
