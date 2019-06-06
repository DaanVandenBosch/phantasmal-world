import React from 'react';
import ReactDOM from 'react-dom';
import './index.less';
import { ApplicationComponent } from './ui/ApplicationComponent';
import 'react-virtualized/styles.css';

ReactDOM.render(
    <ApplicationComponent />,
    document.getElementById('phantasmal-world-root')
);
