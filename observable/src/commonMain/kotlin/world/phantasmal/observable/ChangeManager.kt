package world.phantasmal.observable

object ChangeManager {
    private var currentChangeSet: ChangeSet? = null

    fun inChangeSet(block: () -> Unit) {
        val existingChangeSet = currentChangeSet
        val changeSet = existingChangeSet ?: ChangeSet().also {
            currentChangeSet = it
        }

        try {
            block()
        } finally {
            if (existingChangeSet == null) {
                currentChangeSet = null
                changeSet.complete()
            }
        }
    }

    fun changed(dependency: Dependency) {
        val changeSet = currentChangeSet

        if (changeSet == null) {
            dependency.emitDependencyChanged()
        } else {
            changeSet.changed(dependency)
        }
    }
}

private class ChangeSet {
    private val changedDependencies: MutableList<Dependency> = mutableListOf()

    fun changed(dependency: Dependency) {
        changedDependencies.add(dependency)
    }

    fun complete() {
        for (dependency in changedDependencies) {
            dependency.emitDependencyChanged()
        }
    }
}
