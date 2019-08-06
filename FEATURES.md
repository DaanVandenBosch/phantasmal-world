# Feature Overview

Features that are in ***bold italics*** are planned and not yet implemented.

## Create New Quest

- *Support for episodes I, II and IV*

## Load Quest

- Open file button
- Support for .qst (BB, ***GC***, ***PC***, ***DC***)
- ***Notify user when and why quest loading fails***
  - ***Deal with missing DAT or BIN file in QST container file***

## Save Quest

- Save as button
  - Save as dialog to choose name
- Support for .qst (BB, ***GC***, ***PC***, ***DC***)
- ***Notify user when and why quest saving fails***

## Undo/Redo

- Undo/redo stack
- Undo/redo buttons
- Undo/redo keybindings

## Area Selection

- Dropdown menu to switch area

## Simple Quest Properties

- Episode
- Editable ID, name, short and long description
  - ***Undo/redo***
- NPC counts

## 3D View

- Area geometry
  - Collision geometry (c.rel)
  - ***Rendering geometry (n.rel)***
    - ***Textures***
- NPC/object geometry
  - Textures
- ***Transparency***
  - ***Order independent transparency***
- ***Minimap***
- ***Top-down view (orthogonal view might suffice?)***
- ***Add "shadow" to entities to more easily see where floating entities are positioned***
  - ***MVP: a single line***
- ***Show positions and radii from the relevant script instructions***

## NPC/object manipulation

- ***Creation***
- ***Deletion***
- Translation
  - Via 3D view
  - Via entity view
- ***Rotation***
- ***Multi select and translate/rotate/edit***

## Events

- ***Create events***
- ***Delete events***
- ***Edit events***

## Script Object Code

- Disassembler
- Assembler
- Instructions
- Simplified stack management (push* instructions are inserted transparently)
- Data
  - Binary data
  - Strings
- Labels
- ***Interpret code called from objects as code***

## Script Assembly Editor

- Instructions
- Data
  - Binary data
  - Strings
- Labels
  - ***Show in outline***
- Autocompletion
  - Segment type (.code, .data)
  - Instructions
- ***Go to label***
- ***Warnings***
  - ***Missing 0 label***
  - ***Missing floor handlers***
  - ***Missing map designations***
  - ***Threads (thread, thread_stg) that don't start with a sync***
  - ***Unreachable/unused instructions/data***
    - ***Instructions after "ret" instruction***
    - ***Unused labels***
- Errors
  - Invalid syntax
  - Invalid instruction
  - Invalid instruction arguments
  - ***Invalid label references***
  - ***Mark all duplicate labels (the first one is not marked at the moment)***
- ***Instruction parameter hints***
- ***Show instruction documentation on hover over***
- ***Show reserved register usage on hover over***
- ***When saving, ask user whether to really save when asm contains errors***

## Enemy Waves

- ***Figure out how they work***

## Non-BlueBurst Support

- Support different sets of instructions (older versions had no stack)

## Bugs

- [Script Object Code](#script-object-code): Correctly deal with stack arguments (e.g. when a function expects a u32, pushing a u8, u16, u32 or register value is ok) (when a function expects a register reference, arg_pushb should be used)
- [3D View](#3d-view): Random Type Box 1 and Fixed Type Box objects aren't rendered correctly
- [3D View](#3d-view): Some objects are only partially loaded (they consist of several seperate models)
  - Forest Switch
  - Laser Fence
  - Forest Laser
  - Switch (none door)
  - Energy Barrier
- [Script Object Code](#script-object-code): Make sure data segments are referenced by an instruction with an offset before the segment's offset
- [Script Object Code](#script-object-code): Detect code that is both unused and incorrect and reinterpret it as data (this avoids loading and then saving the quest incorrectly)
- [Area Selection](#area-selection): Lost heart breaker/phantasmal world 4 overwrite area 16 to have both towers
- [Area Selection](#area-selection): Show areas that are referenced from .dat but not from script (test with Point of Disaster (709))
- [Load Quest](#load-quest): Can't parse quest 4
- [Load Quest](#load-quest): Can't parse quest 125 White Day
