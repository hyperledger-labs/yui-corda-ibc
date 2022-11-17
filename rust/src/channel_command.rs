use super::channel;
use super::Result;
use bytes::{BufMut, BytesMut};
use prost::Message;
use std::path::PathBuf;
use structopt::StructOpt;
use tokio::fs;

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

        #[structopt(short, long)]
        output_path: PathBuf,

        #[structopt(short, long)]
        save_whole_response: bool,
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
            output_path,
            save_whole_response,
        } => {
            let response = channel::query_channel(endpoint, port_id, channel_id).await?;
            let mut buf = BytesMut::new();
            if save_whole_response {
                response.encode(&mut buf)?;
            } else {
                buf.put(response.proof.as_slice());
            }
            fs::write(output_path, buf).await?;
        }
    }
    Ok(())
}
