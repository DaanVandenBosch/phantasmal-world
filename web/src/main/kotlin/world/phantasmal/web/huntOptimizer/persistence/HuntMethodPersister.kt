package world.phantasmal.web.huntOptimizer.persistence

import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.persistence.Persister
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import kotlin.time.hours

class HuntMethodPersister : Persister() {
    suspend fun persistMethodUserTimes(huntMethods: List<HuntMethodModel>, server: Server) {
        val userTimes = mutableMapOf<String, Double>()

        for (method in huntMethods) {
            method.userTime.value?.let { userTime ->
                userTimes[method.id] = userTime.inHours
            }
        }

        persistForServer(server, METHOD_USER_TIMES_KEY, userTimes)
    }

    suspend fun loadMethodUserTimes(huntMethods: List<HuntMethodModel>, server: Server) {
        loadForServer<Map<String, Double>>(server, METHOD_USER_TIMES_KEY)?.let { userTimes ->
            for (method in huntMethods) {
                userTimes[method.id]?.let { userTime ->
                    method.setUserTime(userTime.hours)
                }
            }
        }
    }

    companion object {
        private const val METHOD_USER_TIMES_KEY = "HuntMethodStore.methodUserTimes"
    }
}
