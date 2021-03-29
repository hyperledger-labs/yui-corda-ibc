mod admin;
mod admin_command;
mod client;
mod client_command;
mod generated;
mod genesis;
mod genesis_command;
mod host_and_bank;
mod host_and_bank_command;
mod util;

use structopt::StructOpt;

type Result<T> = std::result::Result<T, Box<dyn std::error::Error>>;

#[derive(StructOpt, Debug)]
#[structopt(name = "corda-ibc-client")]
enum Opt {
    Admin(admin_command::Opt),
    Genesis(genesis_command::Opt),
    HostBank(host_and_bank_command::Opt),
    Client(client_command::Opt),
}

#[tokio::main]
async fn main() -> Result<()> {
    match Opt::from_args() {
        Opt::Admin(opt) => admin_command::execute(opt).await?,
        Opt::Genesis(opt) => genesis_command::execute(opt).await?,
        Opt::HostBank(opt) => host_and_bank_command::execute(opt).await?,
        Opt::Client(opt) => client_command::execute(opt).await?,
    }
    Ok(())
}
