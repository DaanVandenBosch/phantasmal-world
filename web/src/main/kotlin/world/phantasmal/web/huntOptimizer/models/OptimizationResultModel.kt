package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.web.shared.dto.ItemType

class OptimizationResultModel(
    val wantedItems: List<ItemType>,
    val optimalMethods: List<OptimalMethodModel>,
)
