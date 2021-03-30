use super::generated::ibc;
use ibc::core::client::v1 as v1client;
use ibc::core::commitment::v1 as v1commitment;
use ibc::core::connection::v1 as v1connection;

pub static HEIGHT: v1client::Height = v1client::Height {
    version_number: 0,
    version_height: 1,
};

lazy_static! {
    pub static ref VERSION: v1connection::Version = v1connection::Version {
        identifier: "1".to_owned(),
        features: vec!["ORDER_ORDERED".to_owned(), "ORDER_UNORDERED".to_owned()],
    };
    pub static ref PREFIX: v1commitment::MerklePrefix = v1commitment::MerklePrefix {
        key_prefix: b"ibc".to_vec(),
    };
}

pub static CLIENT_STATE_TYPE_URL: &str = "/ibc.lightclients.corda.v1.ClientState";
pub static CONSENSUS_STATE_TYPE_URL: &str = "/ibc.lightclients.corda.v1.ConsensusState";
