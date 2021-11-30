package world.phantasmal.web.huntOptimizer.persistence

import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.persistence.KeyValueStore
import world.phantasmal.web.core.persistence.Persister
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import kotlin.time.Duration
import kotlin.time.DurationUnit.HOURS

class HuntMethodPersister(keyValueStore: KeyValueStore) : Persister(keyValueStore) {
    suspend fun persistMethodUserTimes(huntMethods: List<HuntMethodModel>, server: Server) {
        val userTimes = mutableMapOf<String, Double>()

        for (method in huntMethods) {
            method.userTime.value?.let { userTime ->
                userTimes[method.id] = userTime.toDouble(HOURS)
            }
        }

        persistForServer(server, METHOD_USER_TIMES_KEY, userTimes)
    }

    suspend fun loadMethodUserTimes(huntMethods: List<HuntMethodModel>, server: Server) {
        loadForServer<Map<String, Double>>(server, METHOD_USER_TIMES_KEY)?.let { userTimes ->
            for (method in huntMethods) {
                userTimes[method.id]?.let { userTime ->
                    method.setUserTime(Duration.hours(userTime))
                }
            }
        }
    }

    companion object {
        private const val METHOD_USER_TIMES_KEY = "HuntMethodStore.methodUserTimes"
    }
}
