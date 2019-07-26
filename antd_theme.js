module.exports = {
    // Some colors are set to ridiculous values so they stand out once
    // they're first used. They can then be changed to something more
    // sensible.
    "@background-color-base": "hsl(0, 0%, 20%)",
    "@background-color-light": "lighten(@background-color-base, 20%)",
    "@body-background": "@background-color-base",
    "@component-background": "@background-color-base",

    "@text-color": "hsl(0, 0%, 90%)",
    "@text-color-secondary": "hsl(0, 0%, 35%)",
    "@text-color-dark": "hsl(0, 0%, 15%)",
    "@text-color-dark-secondary": "hsl(0, 0%, 35%)",
    "@heading-color": "hsl(0, 0%, 85%)",

    "@item-hover-bg": "hsl(200, 30%, 30%)",
    "@item-active-bg": "hsl(200, 50%, 30%)",

    "@primary-color": "hsl(200, 60%, 75%)",
    // Color used to control the text color in many active and hover states.
    "@primary-5": "hsl(200, 10%, 60%)",
    // Color used to control the text color of active buttons.
    "@primary-6": "hsl(200, 30%, 60%)",
    "@disabled-color": "hsl(0, 0%, 50%)",
    "@tag-default-bg": "yellow",
    "@popover-bg": "yellow",
    "@highlight-color": "blue",
    "@info-color": "orange",
    "@warning-color": "salmon",
    "@alert-message-color": "red",

    "@padding-lg": "12px",
    "@padding-md": "8px",
    "@padding-sm": "6px",
    "@padding-xs": "4px",

    "@layout-body-background": "cyan",
    "@layout-sider-background": "lime",
    "@layout-header-background": "lime",
    "@layout-trigger-color": "magenta",
    "@layout-trigger-background": "purple",

    "@menu-dark-bg": "@component-background",
    "@menu-dark-submenu-bg": "@component-background",

    "@input-bg": "darken(@background-color-base, 5%)",
    "@input-height-base": "28px",
    "@input-height-lg": "34px",
    "@input-height-sm": "24px",

    "@btn-height-base": "28px",
    "@btn-height-lg": "34px",
    "@btn-height-sm": "24px",
    "@btn-default-bg": "lighten(@background-color-base, 10%)",

    "@border-color-base": "lighten(@background-color-base, 20%)",
    "@border-color-split": "lighten(@background-color-base, 10%)",
    "@border-radius-base": "0",
    "@border-radius-sm": "0",

    "@table-selected-row-bg": "@item-active-bg",
    "@table-row-hover-bg": "@item-hover-bg",
    "@collapse-header-bg": "yellow",

    "@tabs-card-head-background": "darken(@background-color-base, 5%)",
    "@tabs-card-height": "28px",
    "@tabs-card-active-color": "white",
    "@tabs-highlight-color": "white",
    "@tabs-hover-color": "white",
    "@tabs-active-color": "white",
    "@tabs-card-active-color": "white",
    "@tabs-ink-bar-color": "white",
};