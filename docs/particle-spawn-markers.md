# Quest Particle Spawn Markers

## Scope

The quest editor previews particle emitters created by quest DAT objects and PSOBB quest-script
opcodes. The preview follows client execution semantics rather than assigning a floor from source
code proximity or naming conventions.

## Client Semantics

PSOBB particle emitters do not store a logical floor ID. An opcode emitter is created in the map
that is loaded when the opcode executes. A floor transition destroys emitters owned by the old map.
A persistent quest thread can subsequently create a new emitter in the new map.

The client provides 18 floor-handler slots, indexed 0 through 17. `set_floor_handler` replaces the
current label in a slot and `clr_floor_handler` clears it.

Quest thread lifetime determines whether execution remains tied to a floor:

- `thread_stg` is reparented to the floor-local quest-thread list and is destroyed during a floor
  transition.
- `thread` remains parented to the Quest object and can resume after a floor transition.
- `StartQuestThreadForCurrentFloor` also creates a persistent Quest-owned thread. The registered
  floor determines its initial execution floor, but not the floor on which it may resume after a
  yield.

Consequently, an opcode particle is not necessarily single-floor. After a resumable opcode such as
`sync`, a persistent thread is analyzed against every possible runtime `g_CurrentFloor`. A later
`get_floor_number` followed by `switch_jmp`, `switch_call`, `jmpi_=`, or `jmpi_!=` can narrow the
execution back to one or more specific floors.

## Editor Attribution

`ParticleSpawn.executionFloorIds` is the set of logical floors on which the client can execute the
spawn instruction. It is not a floor field stored in the emitter.

The analysis begins at client-created entry points:

- label 0 on Pioneer 2 (floor 0);
- active floor handlers on their indexed floor;
- DAT object and scripted NPC callbacks on the entity's DAT `areaId`;
- floor-local spatial, object, chat, and `thread_stg` callbacks;
- quest success, failure, and cancel handlers on Pioneer 2;
- Quest Board handlers on Pioneer 2;
- quest-exit handlers on the runtime exit floor.

Handler writes are propagated through calls and branches. They become externally observable when a
quest thread yields or returns, which prevents a handler overwritten or cleared in the same VM
execution slice from being treated as active.

Instructions after `ret`, unreferenced labels, invalid floor-handler indexes, and callbacks removed
by a clear operation do not produce markers.

DAT particle objects are different from opcode emitters: their floor is the object's DAT `areaId`,
and their lifetime follows the DAT object.

## Server-Initiated Quest Threads

PSOBB ship command `0xAB` contains a 16-bit quest label and calls `quest_start_thread` directly.
The protocol does not require the label to be registered in the QST. Some normal quest requests,
including statistic and BB item-exchange commands, carry success or failure labels that the server
later returns through `0xAB`.

Arbitrary server-selected labels cannot be derived from a QST file. Callback labels explicitly
carried by QST request opcodes can be derived, but are not yet included in particle entry-point
analysis. Until that support is added, particles reachable only through these server responses are
not previewed.

## Particle Sources

The opcode scanner supports the five PSOBB particle opcodes:

- `particle_v3`;
- `particle2`;
- `particle_id_v3`;
- `particle_effect_nc`;
- `player_effect_nc`.

DAT object types `0x0001` and `0x0240` are treated as persistent particle objects when their
particle ID is valid.

Particle definitions are read from `particleentry.dat` and the area-specific
`particleentrya00.dat` through `particleentrya46.dat` tables. Textures and UV metadata come from the
bundled `effect_nt.xvm` resources.

## Validation

The implementation is covered by JVM and Kotlin/JS tests for thread lifetime, yields, floor
dispatch, callback entry points, handler replacement and clearing, DAT sources, binary particle
tables, asset loading, renderer cleanup, and floor-view filtering. The repository-wide Gradle
`check` task must pass before changes are published.
