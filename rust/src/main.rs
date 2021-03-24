pub mod ibc {
    pub mod applications {
        pub mod transfer {
            pub mod v1 {
                include!(concat!(env!("OUT_DIR"), "/ibc.applications.transfer.v1.rs"));
            }
        }
    }
    pub mod core {
        pub mod client {
            pub mod v1 {
                include!(concat!(env!("OUT_DIR"), "/ibc.core.client.v1.rs"));
            }
        }
        pub mod connection {
            pub mod v1 {
                include!(concat!(env!("OUT_DIR"), "/ibc.core.connection.v1.rs"));
            }
        }
        pub mod channel {
            pub mod v1 {
                include!(concat!(env!("OUT_DIR"), "/ibc.core.channel.v1.rs"));
            }
        }
        pub mod commitment {
            pub mod v1 {
                include!(concat!(env!("OUT_DIR"), "/ibc.core.commitment.v1.rs"));
            }
        }
    }
}

pub mod cosmos {
    pub mod base {
        pub mod v1beta1 {
            include!(concat!(env!("OUT_DIR"), "/cosmos.base.v1beta1.rs"));
        }
        pub mod query {
            pub mod v1beta1 {
                include!(concat!(env!("OUT_DIR"), "/cosmos.base.query.v1beta1.rs"));
            }
        }
    }
}

pub mod tendermint {
    pub mod crypto {
        include!(concat!(env!("OUT_DIR"), "/tendermint.crypto.rs"));
    }
}

fn main() {
    println!("Hello, world!");
}
