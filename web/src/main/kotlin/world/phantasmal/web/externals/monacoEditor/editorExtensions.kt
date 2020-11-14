package world.phantasmal.web.externals.monacoEditor

inline operator fun IColors.get(name: String): String =
    asDynamic()[name].unsafeCast<String>()

inline operator fun IColors.set(name: String, value: String) {
    asDynamic()[name] = value
}
