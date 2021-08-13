use super::channel;
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
        connection_id_a: String,

        #[structopt(long)]
        connection_id_b: String,

        #[structopt(long, default_value = "transfer")]
        port_id_a: String,

        #[structopt(long, default_value = "transfer")]
        port_id_b: String,

        #[structopt(long)]
        channel_id_a: String,

        #[structopt(long)]
        channel_id_b: String,
    },
    QueryChannel {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long, default_value = "transfer")]
        port_id: String,

        #[structopt(short, long)]
        channel_id: String,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::Handshake {
            endpoint_a,
            endpoint_b,
            connection_id_a,
            connection_id_b,
            port_id_a,
            port_id_b,
            channel_id_a,
            channel_id_b,
        } => {
            channel::handshake(
                endpoint_a,
                endpoint_b,
                connection_id_a,
                connection_id_b,
                port_id_a,
                port_id_b,
                channel_id_a,
                channel_id_b,
            )
            .await?;
        }
        Opt::QueryChannel {
            endpoint,
            port_id,
            channel_id,
        } => {
            let response = channel::query_channel(endpoint, port_id, channel_id).await?;
            println!("{:?}", response);
        }
    }
    Ok(())
}
