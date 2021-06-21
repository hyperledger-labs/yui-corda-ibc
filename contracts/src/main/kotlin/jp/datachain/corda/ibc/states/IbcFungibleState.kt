package jp.datachain.corda.ibc.states

import jp.datachain.corda.ibc.ics24.Identifier
import net.corda.core.contracts.FungibleState

abstract class IbcFungibleState<T: Any>: IbcState, FungibleState<T> {
    override val id = Identifier("")
}