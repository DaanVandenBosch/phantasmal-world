import { Alert } from 'antd';
import React from 'react';
import './ErrorBoundary.css';

export class ErrorBoundary extends React.Component {
    state = {
        hasError: false
    };

    render() {
        if (this.state.hasError) {
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

    static getDerivedStateFromError(_error: any) {
        return { hasError: true };
    }
}

export function withErrorBoundary(Component: React.ComponentType) {
    return () => <ErrorBoundary><Component /></ErrorBoundary>;
}
