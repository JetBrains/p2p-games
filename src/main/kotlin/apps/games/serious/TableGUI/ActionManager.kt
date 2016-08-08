package apps.games.serious.TableGUI

/**
 * Created by user on 7/1/16.
 */

/**
 * Interface for representation of all actions happening on the bord
 */
abstract class Action(var delay: Float) {
    private val completeDependencies = mutableListOf<Action>()
    private val readyDependencies = mutableListOf<Action>()

    fun update(delta: Float) {
        if (delay > 0) {
            delay -= delta
        }
        if (delay <= 0) {
            delay = 0f
            execute(delta)
        }
    }

    abstract fun execute(delta: Float)

    abstract fun isComplete(): Boolean

    fun executeWhenComplete(action: Action?) {
        if (action != null) {
            synchronized(completeDependencies) {
                completeDependencies.add(action)
            }
        }
    }

    fun executeWhenReady(action: Action?) {
        if (action != null) {
            synchronized(readyDependencies) {
                readyDependencies.add(action)
            }
        }
    }

    fun readyToExcute(): Boolean {
        resolveDependencies()
        return completeDependencies.isEmpty() && readyDependencies.isEmpty()
    }

    private fun resolveDependencies() {
        synchronized(completeDependencies) {
            val completeIterator = completeDependencies.iterator()
            while (completeIterator.hasNext()) {
                val dependency = completeIterator.next()
                if (dependency.isComplete()) {
                    completeIterator.remove()
                }
            }
        }
        synchronized(readyDependencies) {
            val readyIterator = readyDependencies.iterator()
            while (readyIterator.hasNext()) {
                val dependency = readyIterator.next()
                if (dependency.readyToExcute() && dependency.delay <= 0) {
                    readyIterator.remove()
                }
            }
        }
    }

}


class DelayedAction<out R>(delay: Float, val action: () -> (R)) : Action(
        delay) {
    private var finished: Boolean = false
    override fun execute(delta: Float) {
        finished = true
        action()
    }

    override fun isComplete(): Boolean {
        return finished
    }
}

/***
 * CardGUI action manager. Keeps all
 * cards movements
 */
class ActionManager {
    internal val actions = mutableListOf<Action>()
    internal val pending = mutableListOf<Action>()

    fun update(delta: Float) {
        synchronized(actions) {
            synchronized(pending) {
                val pendingIterator = pending.listIterator()
                while (pendingIterator.hasNext()) {
                    val action = pendingIterator.next()
                    if (action.readyToExcute()) {
                        pendingIterator.remove()
                        actions.add(action)
                    }
                }
            }
            val actionsIterator = actions.listIterator()
            while (actionsIterator.hasNext()) {
                val action = actionsIterator.next()
                action.update(delta)
                if (action.isComplete()) {
                    actionsIterator.remove()
                }
            }
        }
    }

    fun add(action: Action) {
        synchronized(pending) {
            pending.add(action)
        }
    }

    fun addAfterLastReady(action: Action) {
        action.executeWhenReady(getLastAction())
        synchronized(pending) {
            pending.add(action)
        }
    }

    fun addAfterLastComplete(action: Action) {
        action.executeWhenComplete(getLastAction())
        synchronized(pending) {
            pending.add(action)
        }
    }

    fun getLastAction(): Action? {
        synchronized(actions) {
            synchronized(pending) {
                if (pending.isNotEmpty()) {
                    return pending.last()
                }
                if (actions.isNotEmpty()) {
                    return actions.last()
                }
                return null
            }
        }

    }

    fun clear() {
        synchronized(actions) {
            actions.clear()
        }
        synchronized(pending) {
            pending.clear()
        }
    }
}