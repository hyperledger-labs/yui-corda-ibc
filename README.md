# Corda-IBC
Corda-IBC is an implementation of IBC modules that run as CorDapp on Corda platform.
The Inter-Blockchain Communication protocol (IBC) verifiably bridges two blockchains but there's no concept of "chain" in the Corda world.
Therefore in this implementation, a group of fixed members (Corda nodes) is regarded as "chain", in other words, one end point of IBC communication.
For now this implementation supports communication between Corda-IBC and Corda-IBC and between Corda-IBC and Fabric-IBC.
It is planned to support communication with Tendermint-IBC (Cosmos SDK).
