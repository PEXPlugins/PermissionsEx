# Commands API

PermissionsEx contains its own Kotlin-first commands API. As with most modern APIs, this creates a parse tree, but with many Kotlin-specific and PEX-specific helpers.

Command processing is split into 3 phase: Tokenizing, Parsing, Execution


## Command Definitions



## Command Handling

### Tokenizing

### Parsing

### Execution

The deepest executor is triggered, plus any intermediary executors that are possibly needed

### Tab completion

### Generating usage messages

## Introspection

- giving brigadier trees
- visiting nodes

## Text extensions

PEX uses some Kotlin extension functions for the Kyori text API
