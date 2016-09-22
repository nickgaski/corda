package com.r3corda.client.fxutils

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.TransformationList
import java.util.*

/**
 * This list type just replays changes propagated from the underlying source list. Used for testing changes.
 */
class ReplayedList<A>(sourceList: ObservableList<A>) : TransformationList<A, A>(sourceList) {

    val replayedList = ArrayList<A>(sourceList)

    override val size: Int get() = replayedList.size

    override fun sourceChanged(c: ListChangeListener.Change<out A>) {

        beginChange()
        while (c.next()) {
            if (c.wasPermutated()) {
                val from = c.from
                val to = c.to
                val permutation = IntArray(to, { c.getPermutation(it) })
                val permutedSubList = ArrayList<A>(to - from)
                for (i in 0 .. (to - from - 1)) {
                    permutedSubList.add(replayedList[permutation[from + i]])
                }
                permutedSubList.forEachIndexed { i, element ->
                    replayedList[from + i] = element
                }
                nextPermutation(from, to, permutation)
            } else if (c.wasUpdated()) {
                for (i in c.from .. c.to - 1) {
                    replayedList[i] = c.list[i]
                    nextUpdate(i)
                }
            } else {
                if (c.wasRemoved()) {
                    // TODO this assumes that if wasAdded() == true then we are adding elements to the getFrom() position
                    val removePosition = c.from
                    for (i in 0 .. c.removedSize - 1) {
                        replayedList.removeAt(removePosition)
                    }
                    nextRemove(c.from, c.removed)
                }
                if (c.wasAdded()) {
                    val addStart = c.from
                    val addEnd = c.to
                    for (i in addStart .. addEnd - 1) {
                        replayedList.add(i, c.list[i])
                    }
                    nextAdd(addStart, addEnd)
                }
            }
        }
        endChange()
    }

    override fun getSourceIndex(index: Int) = index

    override fun get(index: Int) = replayedList[index]
}