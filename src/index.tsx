import React from 'react';
import ReactDOM from 'react-dom';
import { ApplicationComponent } from './ui/ApplicationComponent';
import './index.css';
import "normalize.css";
import "@blueprintjs/core/lib/css/blueprint.css";
import "@blueprintjs/icons/lib/css/blueprint-icons.css";
import { configure } from 'mobx';

configure({
    enforceActions: 'observed'
});

ReactDOM.render(
    <ApplicationComponent />,
    document.getElementById('phantasmal-world-root')
);
