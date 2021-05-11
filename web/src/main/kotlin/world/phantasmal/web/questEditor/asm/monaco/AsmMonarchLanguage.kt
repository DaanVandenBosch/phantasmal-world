package world.phantasmal.web.questEditor.asm.monaco

import world.phantasmal.web.externals.monacoEditor.IMonarchLanguage
import world.phantasmal.web.externals.monacoEditor.IMonarchLanguageTokenizer
import world.phantasmal.web.externals.monacoEditor.set
import world.phantasmal.webui.obj
import kotlin.js.RegExp

object AsmMonarchLanguage : IMonarchLanguage {
    override var defaultToken: String? = "invalid"

    override var tokenizer: IMonarchLanguageTokenizer = obj {
        this["root"] = arrayOf(
            // Strings.
            obj {
                // Unterminated string.
                regex = RegExp('"' + """([^"\\]|\.)*$""")
                action = obj { token = "string.invalid" }
            },
            obj {
                regex = RegExp("\"")
                action = obj {
                    token = "string.quote"
                    bracket = "@open"
                    next = "@string"
                }
            },

            // Registers.
            obj {
                regex = RegExp("""r\d+""")
                action = obj { token = "predefined" }
            },

            // Labels.
            obj {
                regex = RegExp("""[^\s]+:""")
                action = obj { token = "tag" }
            },

            // Numbers.
            obj {
                regex = RegExp("""0x[0-9a-fA-F]+""")
                action = obj { token = "number.hex" }
            },
            obj {
                regex = RegExp("""-?\d+(\.\d+)?(e-?\d+)?""")
                action = obj { token = "number.float" }
            },
            obj {
                regex = RegExp("""-?[0-9]+""")
                action = obj { token = "number" }
            },

            // Section markers.
            obj {
                regex = RegExp("""\.[^\s]+""")
                action = obj { token = "keyword" }
            },

            // Identifiers.
            obj {
                regex = RegExp("""[a-z][a-z0-9_=<>!]*""")
                action = obj { token = "identifier" }
            },

            // Whitespace.
            obj {
                regex = RegExp("""[ \t\r\n]+""")
                action = obj { token = "white" }
            },
//                        obj {
//                            regex = RegExp("""\/\*""")
//                            action = obj { token = "comment"; next = "@comment" }
//                        },
            obj {
                regex = RegExp("\\/\\/.*$")
                action = obj { token = "comment" }
            },

            // Delimiters.
            obj {
                regex = RegExp(",")
                action = obj { token = "delimiter" }
            },
        )

//                    this["comment"] = arrayOf(
//                        obj {
//                            regex = RegExp("""[^/*]+""")
//                            action = obj { token = "comment" }
//                        },
//                        obj {
//                            // Nested comment.
//                            regex = RegExp("""\/\*""")
//                            action = obj { token = "comment"; next = "@push" }
//                        },
//                        obj {
//                            // Nested comment end.
//                            regex = RegExp("""\*/""")
//                            action = obj { token = "comment"; next = "@pop" }
//                        },
//                        obj {
//                            regex = RegExp("""[/*]""")
//                            action = obj { token = "comment" }
//                        },
//                    )

        this["string"] = arrayOf(
            obj {
                regex = RegExp("""[^\\"]+""")
                action = obj { token = "string" }
            },
            obj {
                regex = RegExp("""\\(?:[n\\"])""")
                action = obj { token = "string.escape" }
            },
            obj {
                regex = RegExp("""\\.""")
                action = obj { token = "string.escape.invalid" }
            },
            obj {
                regex = RegExp("\"")
                action = obj {
                    token = "string.quote"
                    bracket = "@close"
                    next = "@pop"
                }
            },
        )
    }
}
