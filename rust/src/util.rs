use super::generated::ibc;
use super::Result;

use ibc::core::commitment::v1 as v1commitment;
use ibc::lightclients::corda::v1 as v1corda;

use prost_types::Any;

pub fn hex_to_base_id(hex: &str) -> Result<v1corda::StateRef> {
    Ok(v1corda::StateRef {
        txhash: Some(v1corda::SecureHash {
            bytes: hex::decode(hex)?,
        }),
        index: 0,
    })
}

pub fn hex_to_public_key(hex: &str) -> Result<v1corda::PublicKey> {
    Ok(v1corda::PublicKey {
        encoded: hex::decode(hex)?,
    })
}

pub fn hex_to_prefix(hex: &str) -> Result<v1commitment::MerklePrefix> {
    Ok(v1commitment::MerklePrefix {
        key_prefix: hex::decode(hex)?,
    })
}

pub fn pack_any(type_url: String, msg: &impl prost::Message) -> Result<Any> {
    let mut buf = Vec::default();
    msg.encode(&mut buf)?;
    Ok(Any {
        type_url,
        value: buf.into(),
    })
}
