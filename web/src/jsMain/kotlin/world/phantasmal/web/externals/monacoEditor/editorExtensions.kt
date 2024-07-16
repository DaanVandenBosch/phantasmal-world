package world.phantasmal.web.externals.monacoEditor

// Inline to generate easier to understand JS.
@Suppress("NOTHING_TO_INLINE")
inline operator fun IColors.get(name: String): String =
    asDynamic()[name].unsafeCast<String>()

// Inline to generate easier to understand JS.
@Suppress("NOTHING_TO_INLINE")
inline operator fun IColors.set(name: String, value: String) {
    asDynamic()[name] = value
}
