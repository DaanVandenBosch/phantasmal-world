package world.phantasmal.webui.dom

@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
internal const val DEFAULT_STYLE = """
#pw-root {
    /* Basic Widget variables */

    --pw-bg-color: hsl(0, 0%, 15%);
    --pw-text-color: hsl(0, 0%, 80%);
    --pw-text-color-disabled: hsl(0, 0%, 55%);
    --pw-font-family: Verdana, Geneva, sans-serif;
    --pw-border-color: hsl(0, 0%, 25%);
    --pw-border-color-focus: hsl(0, 0%, 35%);
    --pw-border: solid 1px var(--pw-border-color);
    --pw-border-focus: solid 1px var(--pw-border-color-focus);

    /* Scrollbars */

    --pw-scrollbar-color: hsl(0, 0%, 13%);
    --pw-scrollbar-thumb-color: hsl(0, 0%, 17%);

    /* Controls */

    --pw-control-bg-color: hsl(0, 0%, 20%);
    --pw-control-bg-color-hover: hsl(0, 0%, 25%);
    --pw-control-text-color: hsl(0, 0%, 80%);
    --pw-control-text-color-hover: hsl(0, 0%, 90%);
    --pw-control-border: solid 1px hsl(0, 0%, 10%);

    --pw-control-inner-border: solid 1px hsl(0, 0%, 35%);
    --pw-control-inner-border-focus: solid 1px hsl(0, 0%, 50%);

    /* Inputs */

    --pw-input-bg-color: hsl(0, 0%, 12%);
    --pw-input-bg-color-disabled: hsl(0, 0%, 15%);
    --pw-input-text-color: hsl(0, 0%, 75%);
    --pw-input-text-color-disabled: var(--pw-text-color-disabled);
    --pw-input-border: solid 1px hsl(0, 0%, 25%);
    --pw-input-border-hover: solid 1px hsl(0, 0%, 30%);
    --pw-input-border-focus: solid 1px hsl(0, 0%, 40%);
    --pw-input-border-disabled: solid 1px hsl(0, 0%, 20%);

    --pw-input-inner-border: solid 1px hsl(0, 0%, 5%);

    /* TabContainer */

    --pw-tab-bg-color: hsl(0, 0%, 12%);
    --pw-tab-bg-color-hover: hsl(0, 0%, 18%);
    --pw-tab-bg-color-active: var(--pw-bg-color);
    --pw-tab-text-color: hsl(0, 0%, 75%);
    --pw-tab-text-color-hover: hsl(0, 0%, 85%);
    --pw-tab-text-color-active: hsl(0, 0%, 90%);

    /* Root element styling */

    cursor: default;
    user-select: none;
    overflow: hidden;
    font-size: 12px;
    background-color: var(--pw-bg-color);
    color: var(--pw-text-color);
    font-family: var(--pw-font-family), sans-serif;
}

.pw-root * {
    scrollbar-color: var(--pw-scrollbar-thumb-color) var(--pw-scrollbar-color);
}

.pw-root ::-webkit-scrollbar {
    background-color: var(--pw-scrollbar-color);
}

.pw-root ::-webkit-scrollbar-track {
    background-color: var(--pw-scrollbar-color);
}

.pw-root ::-webkit-scrollbar-thumb {
    background-color: var(--pw-scrollbar-thumb-color);
}

.pw-root ::-webkit-scrollbar-corner {
    background-color: var(--pw-scrollbar-color);
}

.pw-root h2 {
    font-size: 1.1em;
    margin: 0.5em 0;
}

.pw-root th {
    font-weight: normal;
}

.pw-root *[hidden] {
    display: none;
}
"""
