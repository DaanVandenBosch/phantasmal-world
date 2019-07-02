import React, { PureComponent, ReactNode } from "react";
import {
    OptionValues,
    ReactAsyncSelectProps,
    ReactCreatableSelectProps,
    ReactSelectProps,
} from "react-select";
import VirtualizedSelect, { AdditionalVirtualizedSelectProps } from "react-virtualized-select";
import "./BigSelect.less";

/**
 * Simply wraps {@link VirtualizedSelect} to provide consistent styling.
 */
export class BigSelect<TValue = OptionValues> extends PureComponent<
    VirtualizedSelectProps<TValue>
> {
    render(): ReactNode {
        return <VirtualizedSelect className="BigSelect" {...this.props} />;
    }
}

// Copied from react-virtualized-select.
type VirtualizedSelectProps<TValue = OptionValues> =
    | (ReactCreatableSelectProps<TValue> &
          ReactAsyncSelectProps<TValue> &
          AdditionalVirtualizedSelectProps<TValue> & { async: true })
    | ReactCreatableSelectProps<TValue> &
          ReactSelectProps<TValue> &
          AdditionalVirtualizedSelectProps<TValue>;
