package world.phantasmal.web.externals.monacoEditor

typealias IMonarchLanguageRule = IExpandedMonarchLanguageRule

// Inline to generate easier to understand JS.
@Suppress("NOTHING_TO_INLINE")
inline operator fun IMonarchLanguageTokenizer.get(name: String): Array<IMonarchLanguageRule> =
    asDynamic()[name].unsafeCast<Array<IMonarchLanguageRule>>()

// Inline to generate easier to understand JS.
@Suppress("NOTHING_TO_INLINE")
inline operator fun IMonarchLanguageTokenizer.set(
    name: String,
    value: Array<IMonarchLanguageRule>,
) {
    asDynamic()[name] = value
}

// Inline to generate easier to understand JS.
@Suppress("NOTHING_TO_INLINE")
inline operator fun IMarkdownStringUris.get(name: String): UriComponents =
    asDynamic()[name].unsafeCast<UriComponents>()

// Inline to generate easier to understand JS.
@Suppress("NOTHING_TO_INLINE")
inline operator fun IMarkdownStringUris.set(name: String, value: UriComponents) {
    asDynamic()[name] = value
}
