package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.states.IbcFungibleState
import jp.datachain.corda.ibc.states.IbcState
import net.corda.core.contracts.ContractState

class Context(val inStates: Collection<ContractState>, val refStates: Collection<ContractState>) {
    val outStates = mutableSetOf<ContractState>()

    private val inIbcStates get() = inStates.mapNotNull{it as? IbcState}
    private val refIbcStates get() = refStates.mapNotNull{it as? IbcState}
    private val outIbcStates get() = outStates.mapNotNull{it as? IbcState}

    inline fun <reified T: ContractState> getInputs(): List<T> {
        return inStates.filterIsInstance<T>()
    }
    inline fun <reified T: ContractState> getInput() = getInputs<T>().single()

    inline fun <reified T: ContractState> getReferences(): List<T> {
        return refStates.filterIsInstance<T>()
    }
    inline fun <reified T: ContractState> getReference() = getReferences<T>().single()

    inline fun <reified T: ContractState> addOutput(state: T) {
        assert(outStates.none{it is T})
        outStates.add(state)
    }

    fun verifyResults(expectedOutputStates: Collection<ContractState>) {
        // Confirm that output states are expected ones
        require(expectedOutputStates.size == outStates.size)
        expectedOutputStates.forEach{require(outStates.contains(it)){"$it is not included in outStates"}}

        // Confirm that all input states are included in output states
        require(outIbcStates.filter{it !is IbcFungibleState<*>}.map{it.linearId}.containsAll(
                inIbcStates.filter{it !is IbcFungibleState<*>}.map{it.linearId}))

        // Confirm all baseIds in states are same
        val baseId = outIbcStates.first().baseId
        inIbcStates.forEach{require(it.baseId == baseId)}
        refIbcStates.forEach{require(it.baseId == baseId)}
        outIbcStates.forEach{require(it.baseId == baseId)}
    }
}