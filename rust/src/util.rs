use super::generated::ibc;
use super::Result;

use ibc::lightclients::corda::v1 as ibc_corda;

use prost_types::Any;

pub fn hex_to_base_id(hex: &str) -> Result<ibc_corda::StateRef> {
    Ok(ibc_corda::StateRef {
        txhash: Some(ibc_corda::SecureHash {
            bytes: hex::decode(hex)?,
        }),
        index: 0,
    })
}

pub fn hex_to_public_key(hex: &str) -> Result<ibc_corda::PublicKey> {
    Ok(ibc_corda::PublicKey {
        encoded: hex::decode(hex)?,
    })
}

pub fn pack_any(type_url: &str, msg: &impl prost::Message) -> Result<Any> {
    let mut buf = Vec::default();
    msg.encode(&mut buf)?;
    Ok(Any {
        type_url: type_url.to_owned(),
        value: buf.into(),
    })
}
