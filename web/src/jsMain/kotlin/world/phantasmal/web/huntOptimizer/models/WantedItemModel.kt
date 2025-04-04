package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.web.shared.dto.ItemType

class WantedItemModel(val itemType: ItemType, amount: Int) {
    private val _amount = mutableCell(0)

    val amount: Cell<Int> = _amount

    init {
        setAmount(amount)
    }

    fun setAmount(amount: Int) {
        require(amount >= 0) { "amount should be greater than or equal to 0." }

        _amount.value = amount
    }
}
