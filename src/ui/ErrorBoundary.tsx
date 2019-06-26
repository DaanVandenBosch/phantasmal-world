import { Alert } from 'antd';
import React from 'react';
import './ErrorBoundary.css';

export class ErrorBoundary extends React.Component {
    state = {
        has_error: false
    };

    render() {
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

    static getDerivedStateFromError(_error: any) {
        return { hasError: true };
    }
}

export function with_error_boundary(Component: React.ComponentType) {
    return () => <ErrorBoundary><Component /></ErrorBoundary>;
}
