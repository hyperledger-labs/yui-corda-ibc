use super::client;
use super::Result;
use bytes::{BufMut, BytesMut};
use prost::Message;
use std::path::PathBuf;
use structopt::StructOpt;
use tokio::fs;

#[derive(StructOpt, Debug)]
pub enum Opt {
    CreateClients {
        #[structopt(long, default_value = "http://localhost:9999")]
        endpoint_a: String,

        #[structopt(long, default_value = "http://localhost:9998")]
        endpoint_b: String,
    },
    QueryClientState {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        client_id: String,

        #[structopt(short, long)]
        output_path: PathBuf,

        #[structopt(short, long)]
        save_whole_response: bool,
    },
    QueryConsensusState {
        #[structopt(short, long, default_value = "http://localhost:9999")]
        endpoint: String,

        #[structopt(short, long)]
        client_id: String,

        #[structopt(short = "n", long)]
        version_number: u64,

        #[structopt(short = "h", long)]
        version_height: u64,

        #[structopt(short, long)]
        latest_height: bool,

        #[structopt(short, long)]
        output_path: PathBuf,

        #[structopt(short, long)]
        save_whole_response: bool,
    },
}

pub async fn execute(opt: Opt) -> Result<()> {
    match opt {
        Opt::CreateClients {
            endpoint_a,
            endpoint_b,
        } => {
            client::create_clients(endpoint_a, endpoint_b).await?;
        }
        Opt::QueryClientState {
            endpoint,
            client_id,
            output_path,
            save_whole_response,
        } => {
            let response = client::query_client_state(endpoint, client_id).await?;
            let mut buf = BytesMut::new();
            if save_whole_response {
                response.encode(&mut buf)?;
            } else {
                buf.put(response.proof.as_slice());
            }
            fs::write(output_path, buf).await?;
        }
        Opt::QueryConsensusState {
            endpoint,
            client_id,
            version_number,
            version_height,
            latest_height,
            output_path,
            save_whole_response,
        } => {
            let response = client::query_consensus_state(
                endpoint,
                client_id,
                version_number,
                version_height,
                latest_height,
            )
            .await?;
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
