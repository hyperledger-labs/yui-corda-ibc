use super::connection;
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
            output_path,
            save_whole_response,
        } => {
            let response = connection::query_connection(endpoint, connection_id).await?;
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
