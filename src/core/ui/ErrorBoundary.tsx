import { Alert } from "antd";
import React, { ReactNode, Component, ComponentType } from "react";
import styles from "./ErrorBoundary.css";

type State = { has_error: boolean };

export class ErrorBoundary extends Component<{}, State> {
    state = {
        has_error: false,
    };

    render(): ReactNode {
        if (this.state.has_error) {
            return (
                <div className={styles.main}>
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

export function with_error_boundary<P>(Component: ComponentType<P>): ComponentType<P> {
    const ComponentErrorBoundary = (props: P): JSX.Element => (
        <ErrorBoundary>
            <Component {...props} />
        </ErrorBoundary>
    );
    ComponentErrorBoundary.displayName = `${Component.displayName}ErrorBoundary`;
    return ComponentErrorBoundary;
}
