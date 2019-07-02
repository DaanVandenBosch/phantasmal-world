import { Alert } from "antd";
import React, { ReactNode, Component, ComponentType } from "react";
import "./ErrorBoundary.css";

type State = { has_error: boolean };

export class ErrorBoundary extends Component<{}, State> {
    state = {
        has_error: false,
    };

    render(): ReactNode {
        if (this.state.has_error) {
            return (
                <div className="ErrorBoundary-error">
                    <div>
                        <Alert type="error" message="Something went wrong." />
                    </div>
                </div>
            );
        } else {
            return this.props.children;
        }
    }

    static getDerivedStateFromError(): State {
        return { has_error: true };
    }
}

export function with_error_boundary(Component: ComponentType): ComponentType {
    const ComponentErrorBoundary = (): JSX.Element => (
        <ErrorBoundary>
            <Component />
        </ErrorBoundary>
    );
    ComponentErrorBoundary.displayName = `${Component.displayName}ErrorBoundary`;
    return ComponentErrorBoundary;
}
