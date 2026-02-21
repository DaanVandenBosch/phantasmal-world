package world.phantasmal.psolib.compatibility

/**
 * Result of a compatibility check for a specific PSO version.
 */
data class CompatibilityResult(
    val version: PSOVersion,
    val errors: List<CompatibilityProblem> = emptyList(),
    val warnings: List<CompatibilityProblem> = emptyList(),
) {
    val isFullyCompatible: Boolean
        get() = errors.isEmpty() && warnings.isEmpty()

    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()
}

/**
 * A compatibility problem (error or warning).
 */
data class CompatibilityProblem(
    val type: ProblemType,
    val message: String,
    val location: ProblemLocation? = null,
)

/**
 * Type of compatibility problem.
 */
enum class ProblemType {
    // Script problems
    MISSING_LABEL_0,
    UNKNOWN_OPCODE,
    OPCODE_VERSION_MISMATCH,
    EPISODE_NOT_SUPPORTED,
    INVALID_ARGUMENT,
    LABEL_NOT_FOUND,
    SWITCH_ARRAY_MISMATCH,
    CONVERSION_WARNING,
    SPECIAL_OPCODE_WARNING,

    // NPC/Monster problems
    NPC_ACTION_LABEL_NOT_FOUND,
    NPC_SCRIPT_LABEL_NOT_SUPPORTED,  // Custom script labels only work in BB (v4)
    SKIN_NOT_SUPPORTED,
    SKIN_51_INVALID_SUBTYPE,
    MONSTER_FLOOR_MISMATCH,
    TOO_MANY_MONSTERS,

    // Object problems
    OBJECT_FLOOR_MISMATCH,
    TOO_MANY_OBJECTS,

    // Data problems
    UNUSED_DATA_LABEL,

    // File problems
    MISSING_BIN_FILE,
    MISSING_DAT_FILE,
    PVR_NOT_SUPPORTED,
}

/**
 * Location of a compatibility problem.
 */
sealed class ProblemLocation {
    data class ScriptLine(val lineNumber: Int) : ProblemLocation()
    data class Monster(val index: Int, val floorId: Int) : ProblemLocation()
    data class Object(val index: Int, val floorId: Int) : ProblemLocation()
    data class Floor(val floorId: Int) : ProblemLocation()
    data class DataLabel(val label: String) : ProblemLocation()
}

/**
 * Builder for creating CompatibilityResult.
 */
class CompatibilityResultBuilder(private val version: PSOVersion) {
    private val errors = mutableListOf<CompatibilityProblem>()
    private val warnings = mutableListOf<CompatibilityProblem>()

    fun addError(type: ProblemType, message: String, location: ProblemLocation? = null) {
        errors.add(CompatibilityProblem(type, message, location))
    }

    fun addWarning(type: ProblemType, message: String, location: ProblemLocation? = null) {
        warnings.add(CompatibilityProblem(type, message, location))
    }

    fun build(): CompatibilityResult = CompatibilityResult(version, errors.toList(), warnings.toList())
}