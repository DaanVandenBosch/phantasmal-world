package world.phantasmal.web.externals.monacoEditor

typealias IMonarchLanguageRule = IExpandedMonarchLanguageRule

inline operator fun IMonarchLanguageTokenizer.get(name: String): Array<IMonarchLanguageRule> =
    asDynamic()[name].unsafeCast<Array<IMonarchLanguageRule>>()

inline operator fun IMonarchLanguageTokenizer.set(name: String, value: Array<IMonarchLanguageRule>) {
    asDynamic()[name] = value
}
