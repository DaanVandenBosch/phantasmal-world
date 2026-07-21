package world.phantasmal.web.questEditor.widgets

import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.battleparam.BattleParamDifficulty
import world.phantasmal.psolib.battleparam.BattleParamSet
import world.phantasmal.psolib.battleparam.BattleParamTable
import world.phantasmal.psolib.battleparam.EnemyTemplateCatalog
import world.phantasmal.web.questEditor.loading.BattleParamRepository
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Dialog
import world.phantasmal.webui.widgets.Select
import world.phantasmal.webui.widgets.Widget

/**
 * Template browser modal opened from the `Load template…` button on each enemy
 * data dialog.
 *
 * Hosts the Set / Difficulty / Enemy selectors plus, for [TemplateKind.Attack]
 * with multi-variant enemies, an additional Variant selector (only shown when
 * the current enemy has more than one attack form).
 *
 * On OK: resolves the slot via [EnemyTemplateCatalog] and invokes [onApply]
 * with the looked-up [BattleParamTable] + difficulty + slot. The host dialog
 * is responsible for reading the relevant struct (Physical / Attack / Resist /
 * Movement) and copying it into its editable fields — the parent form's data
 * stays unsaved until the user presses OK there.
 */
class LoadTemplateDialog(
    visible: Cell<Boolean>,
    private val repo: BattleParamRepository,
    private val kind: TemplateKind,
    private val onApply: (TemplateLookup) -> Unit,
    onDismiss: () -> Unit,
) : Dialog(
    visible = visible,
    title = cell("Load template"),
    description = cell("Pick a BattleParamEntry source and enemy slot."),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    enum class TemplateKind { Physical, Attack, Resist, Movement }

    private val selectedSet = mutableCell(BattleParamSet.Ep1Online)
    private val selectedDifficulty = mutableCell(BattleParamDifficulty.Normal)

    private val enemyNames: Cell<List<String>> = selectedSet.map { set ->
        EnemyTemplateCatalog.namesFor(set.episode)
    }
    private val selectedEnemy = mutableCell<String?>(null)

    private val attackVariants: Cell<List<EnemyTemplateCatalog.AttackVariant>> =
        map(selectedSet, selectedEnemy) { set, name ->
            if (name == null) emptyList()
            else EnemyTemplateCatalog.attackVariants(set.episode, name) ?: emptyList()
        }
    private val selectedVariant = mutableCell<EnemyTemplateCatalog.AttackVariant?>(null)

    init {
        // These reset cells in response to a derived cell change, so they fire
        // during the leaf-notification phase of the upstream mutation. Defer
        // the writes via mutateDeferred to avoid the same re-entrant
        // notification bug fixed in EnemyPhysicalDataDialog.
        observeNow(enemyNames) { names ->
            val current = selectedEnemy.value
            if (current == null || current !in names) {
                mutateDeferred { selectedEnemy.value = names.firstOrNull() }
            }
        }
        observeNow(attackVariants) { variants ->
            mutateDeferred { selectedVariant.value = variants.firstOrNull() }
        }

        val bodyElement = dialogElement.querySelector(".pw-dialog-body")
        bodyElement?.let { body ->
            body.innerHTML = ""
            val contentWidget = addDisposable(Content())
            body.appendChild(contentWidget.element)
        }

        val footerElement = dialogElement.querySelector(".pw-dialog-footer")
        footerElement?.let { footer ->
            footer.innerHTML = ""
            val okBtn = addDisposable(Button(
                text = "OK",
                enabled = repo.available,
                onClick = {
                    if (applyTemplate()) {
                        onDismiss()
                    }
                },
            ))
            footer.appendChild(okBtn.element)
            val cancelBtn = addDisposable(Button(text = "Cancel", onClick = { onDismiss() }))
            footer.appendChild(cancelBtn.element)
        }

        dialogElement.style.width = "360px"
    }

    private fun applyTemplate(): Boolean {
        val set = selectedSet.value
        val table: BattleParamTable = repo.get(set) ?: return false
        val episode = set.episode
        val enemy = selectedEnemy.value ?: return false
        val enemyIndex = EnemyTemplateCatalog.namesFor(episode).indexOf(enemy)
        if (enemyIndex < 0) return false

        val ids: IntArray = when (kind) {
            TemplateKind.Physical -> EnemyTemplateCatalog.physIdsFor(episode)
            TemplateKind.Attack   -> EnemyTemplateCatalog.attackIdsFor(episode)
            TemplateKind.Resist   -> EnemyTemplateCatalog.resistIdsFor(episode)
            TemplateKind.Movement -> EnemyTemplateCatalog.movementIdsFor(episode)
        }
        val baseSlot = ids.getOrNull(enemyIndex) ?: return false
        val slot = if (kind == TemplateKind.Attack) {
            selectedVariant.value?.slotOverride ?: baseSlot
        } else baseSlot

        // "No entry" sentinel — silently no-op.
        if (slot == EnemyTemplateCatalog.NO_SLOT || slot >= BattleParamTable.SLOTS) return false

        onApply(TemplateLookup(table, selectedDifficulty.value.ordinal, slot))
        return true
    }

    data class TemplateLookup(
        val table: BattleParamTable,
        val difficulty: Int,
        val slot: Int,
    )

    private inner class Content : Widget() {
        override fun org.w3c.dom.Node.createElement() =
            div {
                className = "pw-load-template-content"

                row("Set") {
                    addChild(Select(
                        items = cell(BattleParamSet.entries),
                        itemToString = { it.displayName },
                        selected = selectedSet,
                        onSelect = { selectedSet.value = it },
                    ))
                }
                row("Difficulty") {
                    addChild(Select(
                        items = cell(BattleParamDifficulty.entries),
                        itemToString = { it.displayName },
                        selected = selectedDifficulty,
                        onSelect = { selectedDifficulty.value = it },
                    ))
                }
                row("Enemy") {
                    addChild(Select(
                        items = enemyNames,
                        itemToString = { it },
                        selected = selectedEnemy,
                        onSelect = { selectedEnemy.value = it },
                    ))
                }
                if (kind == TemplateKind.Attack) {
                    // Only meaningful when the current enemy has multiple
                    // attack forms (Hildebear, Ill Gill, …).
                    row("Attack") {
                        addChild(Select(
                            visible = attackVariants.map { it.size > 1 },
                            items = attackVariants,
                            itemToString = { it.label },
                            selected = selectedVariant,
                            onSelect = { selectedVariant.value = it },
                        ))
                    }
                }
            }

        private fun org.w3c.dom.Node.row(label: String, fill: org.w3c.dom.Node.() -> Unit) {
            div {
                className = "pw-load-template-row"
                span {
                    className = "pw-load-template-row-label"
                    textContent = label
                }
                fill()
            }
        }
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-load-template-content {
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                    padding: 4px 2px;
                }
                .pw-load-template-row {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }
                .pw-load-template-row-label {
                    width: 80px;
                    flex-shrink: 0;
                }
                .pw-load-template-row .pw-select {
                    flex: 1;
                    width: auto;
                }
            """.trimIndent())
        }
    }
}
