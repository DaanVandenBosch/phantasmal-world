package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.models.ItemType

class WantedItemModel(val itemType: ItemType, amount: Int) {
    private val _amount = mutableVal(0)

    val amount: Val<Int> = _amount

    init {
        setAmount(amount)
    }

    fun setAmount(amount: Int) {
        require(amount >= 0) { "amount should be greater than or equal to 0." }

        _amount.value = amount
    }
}
