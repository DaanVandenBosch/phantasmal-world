# web

This is the main Phantasmal World web application. It consists of several tools, each in their own
package. Beside these packages there's also an application, core and externals package.

## Main Packages

### application

The application package contains the main application view that provides navigation between the
different tools. The application view lazily loads and initializes the necessary tools.

### core

Contains code that is reused throughout the web project.

### externals

External declarations for NPM dependencies.

### huntOptimizer, questEditor, viewer

One main package per tool. Each tool is encapsulated in a PwTool implementation.

## Common Structure

The main packages all follow the same structure except for the externals package.

### widgets

The widgets package contains views with minimal logic. They simply display the models their
controller provides and forward user input to their controller. Their only dependency is the DOM and
a single controller.

Keeping logic out of the views makes the UI easier to test. We don't really need to have unit tests
for the views as they don't contain complex code, just having unit tests from controller layer down
and manually smoke testing the GUI layer gives us enough confidence that everything works.

### controllers

The controllers package contains the controllers on which views depend. Usually the view-controller
relationship is one-to-one, sometimes it's many-to-one
(e.g. when a view has many subviews that work with the same data). A controller usually extracts
data from a shared store and transforms it into a format which the view can easily consume. A
controller has no knowledge of the GUI layer.

### models

The models package contains observable model objects. Models expose read-only cell properties
and allow their properties to be changed via setters which validate their inputs.

### stores

The stores package contains shared data stores. Stores ensure that data is loaded when necessary and
that the data is deduplicated. Stores also contain ephemeral shared state such as the currently
selected entity in the quest editor.

## Subprojects

### web:assembly-worker

Does analysis of the script assembly code and runs in a worker thread.

### web:assets-generation

This code is manually run to generate various assets used by web such as item lists, drop tables,
quest lists, etc.

### web:shared

Contains code used by web, web:assembly-worker and web:assets-generation.
