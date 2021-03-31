pub mod ibc {
    pub mod applications {
        pub mod transfer {
            pub mod v1 {
                tonic::include_proto!("ibc.applications.transfer.v1");
            }
        }
    }
    pub mod core {
        pub mod client {
            pub mod v1 {
                tonic::include_proto!("ibc.core.client.v1");
            }
        }
        pub mod connection {
            pub mod v1 {
                tonic::include_proto!("ibc.core.connection.v1");
            }
        }
        pub mod channel {
            pub mod v1 {
                tonic::include_proto!("ibc.core.channel.v1");
            }
        }
        pub mod commitment {
            pub mod v1 {
                tonic::include_proto!("ibc.core.commitment.v1");
            }
        }
    }
    pub mod lightclients {
        pub mod corda {
            pub mod v1 {
                tonic::include_proto!("ibc.lightclients.corda.v1");
            }
        }
    }
}

pub mod cosmos {
    pub mod base {
        pub mod v1beta1 {
            tonic::include_proto!("cosmos.base.v1beta1");
        }
        pub mod query {
            pub mod v1beta1 {
                tonic::include_proto!("cosmos.base.query.v1beta1");
            }
        }
    }
}

pub mod tendermint {
    pub mod crypto {
        tonic::include_proto!("tendermint.crypto");
    }
}
