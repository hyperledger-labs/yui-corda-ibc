use super::Result;
use prost_types::Any;
use std::collections::HashMap;

pub fn pack_any(type_url: String, msg: &impl prost::Message) -> Result<Any> {
    let mut buf = Vec::default();
    msg.encode(&mut buf)?;
    Ok(Any {
        type_url,
        value: buf.into(),
    })
}

pub fn vec_to_map(entries: &Vec<String>) -> HashMap<String, String> {
    entries
        .iter()
        .map(|ent| {
            let (k, v) = ent.split_once(':').unwrap();
            (k.to_owned(), v.to_owned())
        })
        .collect()
}
