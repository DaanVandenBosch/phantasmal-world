package world.phantasmal

val EXPERIMENTAL_ANNOTATIONS: List<String> = listOf(
    "kotlin.RequiresOptIn",
    "kotlin.ExperimentalUnsignedTypes",
    "kotlin.contracts.ExperimentalContracts",
    "kotlin.time.ExperimentalTime",
)

val EXPERIMENTAL_ANNOTATION_COMPILER_ARGS: List<String> =
    EXPERIMENTAL_ANNOTATIONS.map { "-Xopt-in=$it" }
