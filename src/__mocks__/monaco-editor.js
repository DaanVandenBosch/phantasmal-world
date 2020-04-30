/* eslint-disable @typescript-eslint/no-empty-function,@typescript-eslint/explicit-function-return-type */
class Editor {
    addCommand() {}
    getAction() {}
    addAction() {
        return { dispose() {} };
    }
    trigger() {}
    updateOptions() {}
    setModel() {}
    setPosition() {}
    getLineDecorations() {}
    deltaDecorations() {}
    revealLineInCenterIfOutsideViewport() {}
    revealPositionInCenterIfOutsideViewport() {}
    onDidFocusEditorWidget() {
        return { dispose() {} };
    }
    onMouseDown() {
        return { dispose() {} };
    }
    onMouseUp() {
        return { dispose() {} };
    }
    focus() {}
    layout() {}
    onDidChangeCursorPosition() {
        return { dispose() {} };
    }
    dispose() {}
}

exports.editor = {
    defineTheme() {},
    createModel() {},
    create() {
        return new Editor();
    },
};

exports.languages = {
    CompletionItemKind: {
        Method: 0,
        Function: 1,
        Constructor: 2,
        Field: 3,
        Variable: 4,
        Class: 5,
        Struct: 6,
        Interface: 7,
        Module: 8,
        Property: 9,
        Event: 10,
        Operator: 11,
        Unit: 12,
        Value: 13,
        Constant: 14,
        Enum: 15,
        EnumMember: 16,
        Keyword: 17,
        Text: 18,
        Color: 19,
        File: 20,
        Reference: 21,
        Customcolor: 22,
        Folder: 23,
        TypeParameter: 24,
        Snippet: 25,
    },
    register() {},
    setMonarchTokensProvider() {},
    registerCompletionItemProvider() {},
    registerSignatureHelpProvider() {},
    setLanguageConfiguration() {},
    registerDefinitionProvider() {},
    registerHoverProvider() {},
};

exports.KeyMod = {
    CtrlCmd: 0,
    Shift: 1,
    Alt: 2,
    WinCtrl: 3,
};

exports.KeyCode = {
    LeftArrow: 15,
    UpArrow: 16,
    RightArrow: 17,
    DownArrow: 18,
};
