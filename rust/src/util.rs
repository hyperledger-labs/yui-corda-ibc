use super::Result;
use prost_types::Any;

pub fn pack_any(type_url: String, msg: &impl prost::Message) -> Result<Any> {
    let mut buf = Vec::default();
    msg.encode(&mut buf)?;
    Ok(Any {
        type_url,
        value: buf.into(),
    })
}
