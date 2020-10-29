package jp.datachain.corda.ibc.ics26

import jp.datachain.corda.ibc.states.IbcState

class Context(val inStates: Collection<IbcState>, val refStates: Collection<IbcState>) {
    val outStates = mutableSetOf<IbcState>()

    inline fun <reified T: IbcState> getInput(): T {
        return inStates.filter{it is T}.single() as T
    }

    inline fun <reified T: IbcState> getReference(): T {
        return refStates.filter{it is T}.single() as T
    }

    inline fun <reified T: IbcState> addOutput(state: T) {
        assert(outStates.none{it is T})
        outStates.add(state)
    }

    fun verifyResults(expectedStates: Collection<IbcState>) {
        // Confirm that output states are expected ones
        require(expectedStates.map{it}.sortedBy{it.id}
                == outStates.map{it}.sortedBy{it.id})

        // Confirm that all input states are included in output states
        require(outStates.map{it.linearId}.containsAll(
                inStates.map{it.linearId}))

        // Confirm all baseIds in states are same
        val baseId = outStates.first().baseId
        inStates.forEach{require(it.baseId == baseId)}
        refStates.forEach{require(it.baseId == baseId)}
        outStates.forEach{require(it.baseId == baseId)}
    }
}