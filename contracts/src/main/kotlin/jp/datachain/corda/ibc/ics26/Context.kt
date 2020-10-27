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

    fun matchesOutputs(states: Collection<IbcState>): Boolean {
        return states.size == outStates.size && states.all{outStates.contains(it)}
    }

    fun outputsContainAllInputs(): Boolean {
        return outStates.map{it.linearId}.containsAll(inStates.map{it.linearId})
    }
}