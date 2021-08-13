use super::connection;
use super::Result;
use structopt::StructOpt;

#[derive(StructOpt, Debug)]
pub enum Opt {
    Handshake {
        #[structopt(long, default_value = "http://localhost:9999")]
        endpoint_a: String,

        #[structopt(long, default_value = "http://localhost:9998")]
        endpoint_b: String,

        #[structopt(long)]
        client_id_a: String,

        #[structopt(long)]
        client_id_b: String,

        #[structopt(long)]
        connection_id_a: String,

        #[structopt(long)]
        connection_id_b: String,
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
        Opt::Handshake {
            endpoint_a,
            endpoint_b,
            client_id_a,
            client_id_b,
            connection_id_a,
            connection_id_b,
        } => {
            connection::handshake(
                endpoint_a,
                endpoint_b,
                client_id_a,
                client_id_b,
                connection_id_a,
                connection_id_b,
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
