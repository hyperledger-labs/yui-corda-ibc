use super::generated::ibc;
use super::util;
use super::Result;

use ibc::core::client::v1 as ibc_client;
use ibc::lightclients::corda::v1 as ibc_corda;

pub async fn create_corda_client(
    endpoint: &str,
    client_id: &str,
    counterparty_base_hash: &str,
    counterparty_notary_key: &str,
) -> Result<()> {
    let mut client = ibc_client::msg_client::MsgClient::connect(endpoint.to_owned()).await?;

    let client_state = util::pack_any(
        "/ibc.lightclients.corda.v1.ClientState",
        &ibc_corda::ClientState {
            id: client_id.to_owned(),
        },
    )?;

    let consensus_state = util::pack_any(
        "/ibc.lightclients.corda.v1.ConsensusState",
        &ibc_corda::ConsensusState {
            base_id: Some(util::hex_to_base_id(counterparty_base_hash)?),
            notary_key: Some(util::hex_to_public_key(counterparty_notary_key)?),
        },
    )?;

    client
        .create_client(ibc_client::MsgCreateClient {
            client_id: client_id.to_owned(),
            client_state: Some(client_state),
            consensus_state: Some(consensus_state),
            signer: String::default(),
        })
        .await?;
    Ok(())
}
