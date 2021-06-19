package world.phantasmal.observable

object ChangeManager {
    private var currentChangeSet: ChangeSet? = null

    fun inChangeSet(block: () -> Unit) {
        // TODO: Figure out change set bug and enable change sets again.
//        val existingChangeSet = currentChangeSet
//        val changeSet = existingChangeSet ?: ChangeSet().also {
//            currentChangeSet = it
//        }
//
//        try {
            block()
//        } finally {
//            if (existingChangeSet == null) {
//                // Set to null so changed calls are turned into emitDependencyChanged calls
//                // immediately instead of being deferred.
//                currentChangeSet = null
//                changeSet.complete()
//            }
//        }
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
    private var completing = false
    private val changedDependencies: MutableList<Dependency> = mutableListOf()

    fun changed(dependency: Dependency) {
        check(!completing)

        changedDependencies.add(dependency)
    }

    fun complete() {
        try {
            completing = true

            for (dependency in changedDependencies) {
                dependency.emitDependencyChanged()
            }
        } finally {
            completing = false
        }
    }
}
