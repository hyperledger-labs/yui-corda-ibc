use super::connection;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    OpenInit {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(long)]
        client_id: String,

        #[structopt(long)]
        connection_id: String,

        #[structopt(long)]
        counterparty_client_id: String,

        #[structopt(long)]
        counterparty_connection_id: String,

        #[structopt(long)]
        counterparty_prefix: String,

        #[structopt(long)]
        version_identifier: String,

        #[structopt(long)]
        version_features: Vec<String>,
    },
    QueryConnection {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        connection_id: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::OpenInit {
            endpoint,
            client_id,
            connection_id,
            counterparty_client_id,
            counterparty_connection_id,
            counterparty_prefix,
            version_identifier,
            version_features,
        } => {
            connection::open_init(
                endpoint,
                client_id,
                connection_id,
                counterparty_client_id,
                counterparty_connection_id,
                counterparty_prefix,
                version_identifier,
                version_features,
            )
            .await?;
        }
        Opt::QueryConnection {
            endpoint,
            connection_id,
        } => {
            let response = connection::query_connection(endpoint, connection_id).await?;
            println!("{:?}", response);
        }
    }
    Ok(())
}
