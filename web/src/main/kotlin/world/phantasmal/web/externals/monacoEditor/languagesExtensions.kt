package world.phantasmal.web.externals.monacoEditor

typealias IMonarchLanguageRule = IExpandedMonarchLanguageRule

inline operator fun IMonarchLanguageTokenizer.get(name: String): Array<IMonarchLanguageRule> =
    asDynamic()[name].unsafeCast<Array<IMonarchLanguageRule>>()

inline operator fun IMonarchLanguageTokenizer.set(
    name: String,
    value: Array<IMonarchLanguageRule>,
) {
    asDynamic()[name] = value
}

inline operator fun IMarkdownStringUris.get(name: String): UriComponents =
    asDynamic()[name].unsafeCast<UriComponents>()

inline operator fun IMarkdownStringUris.set(name: String, value: UriComponents) {
    asDynamic()[name] = value
}
