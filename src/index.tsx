import React from 'react';
import ReactDOM from 'react-dom';
import './index.less';
import { ApplicationComponent } from './ui/ApplicationComponent';
import 'react-virtualized/styles.css';
import "react-select/dist/react-select.css";
import "react-virtualized-select/styles.css";

ReactDOM.render(
    <ApplicationComponent />,
    document.getElementById('phantasmal-world-root')
);
