# Feature Overview

Features that are in ***bold italics*** are planned but not yet implemented.

## Create New Quest

- Load a new quest with minimal script and default entities
- Support for episodes I, ***II*** and ***IV***

## Load Quest

- Open file button
- Support for .qst (BB, GC, PC, DC)
- Notify user when and why quest loading fails

## Save Quest

- "Save as" button
    - "Save as" dialog to choose name
- Support for .qst (BB, GC, ***PC***, ***DC***)
- Notify user when and why quest saving fails
- ***Custom text-based format***
    - ***Usable with SCM tools***

## Undo/Redo

- Undo/redo stack
- Undo/redo buttons
- Undo/redo key bindings

## Area Selection

- Dropdown menu to switch area
- Change area variant by editing assembly
    - Update 3D view automatically
- ***Easy navigation between far apart rooms***

## Simple Quest Properties

- Episode
- Editable ID, name, short and long description
    - Undo/redo
- NPC counts

## 3D View

- Area geometry
    - Collision geometry (c.rel)
    - Rendering geometry (n.rel)
        - Textures
        - ***Hide roofs and walls***
- NPC/object geometry
    - Textures
- Transparency
    - ***Order independent transparency***
- ***Mini-map***
- ***Top-down, simplified view (orthogonal view might suffice?)***
- ***Add "shadow" to entities to more easily see where floating entities are positioned***
    - ***MVP: a single line***
- ***Show positions and radii from the relevant script instructions***

## NPC/Object Manipulation

- Creation
    - Drag and drop from a list of NPCs/objects
- Deletion
    - "Delete" key binding
- Translation
    - Via 3D view
    - Via entity view
- Rotation
    - Via 3D view
    - Via entity view
- ***Multi select and translate/rotate/edit***
- Edit entity-specific properties
    - ***Configure entity-specific properties***

## Events

- Event graph
- Add events
- Delete event
    - ***Delete coupled NPCs if requested***
- ***Easy navigations to parent and child events***
- Edit event delay
- ***Go to related event via entity view***

### Event Actions

- Add/Delete
- Lock/unlock doors
- Spawn NPCs
- ***Reorder actions***

## Script Byte Code

- Disassembler
- Assembler
- Instructions
- Simplified stack management (push* instructions are inserted transparently)
- Data
    - Binary data
        - ***Interpret data of known type***
    - Strings
- Labels
- Interpret code called from NPCs and objects as code
- Interpret segments of unknown type as code if possible

## Script Assembly Editor

- Instructions
- Data
    - Binary data
        - ***Interpret data of known type***
    - Strings
- Labels
    - ***Show in outline***
    - Go to label
- Auto-completion
    - Segment type (.code, .data)
    - Instructions
- ***Warnings***
    - ***Missing 0 label***
    - ***Missing floor handlers***
    - ***Missing map designations***
    - ***Infinite loops without sync***
    - ***Unreachable/unused instructions/data***
        - ***Instructions after "ret" instruction***
        - ***Unused labels***
    - Unnecessary section markers
- Errors
    - Invalid syntax
    - Invalid instruction
    - Invalid instruction arguments
    - ***Invalid label references***
    - ***Mark all duplicate labels (the first one is not marked at the moment)***
- Instruction parameter hints
- Show instruction documentation on hover over
- ***Show reserved register usage on hover over***
- ***When saving, ask user whether to really save when asm contains errors***
- ***Theme selection***
- ***Easily switch between segment types***

## Debugger

- Start debugging by clicking "Debug" or pressing F5
- Stop debugging by clicking "Stop" or pressing Shift-F5
- Step with "Step over" (F8), "Step into" (F7) and "Step out" (Shift-F8)
- Continue to next breakpoint with "Continue" (F6)
- Set breakpoints in the script editor
- Register viewer
- Log window
- ***Virtual machine to execute the script***
- ***Quest runner to interact with the game world***

## Non-BlueBurst Support

- ***Support different sets of instructions (older versions had no stack)***

## Verification/Warnings

- ***Entities with nonexistent event section***
- ***Entities with wave that's never triggered***
- ***Duplicate event IDs***
- ***Events with nonexistent event section***
- ***Event waves with no enemies***
- ***Events that trigger nonexistent events***
- ***Events that lock/unlock nonexistent doors***

## Bugs

- When a modal dialog is open, global keybindings should be disabled
- Improve the default camera target for Crater Interior
- Creating a new quest discards changes to the previously open quest without asking user
- Opening a new file discards changes to the previously open quest without asking user
- Toggling "Inline args" clears the undo stack
- Entities with rendering issues:
    - Caves 4 Button door
    - Pofuilly Slime
    - Pouilly Slime
    - Easter Egg
    - Christmas Tree
    - Halloween Pumpkin
    - 21st Century
    - Light rays - used in forest and CCA
    - Big CCA Door Switch
    - Laser Detect - used in CCA
    - Wide Glass Wall (breakable) - used in Seabed
    - item box cca
    - Desert Fixed Type Box (Breakable Crystals)
    - Merissa A
    - Merissa AA
