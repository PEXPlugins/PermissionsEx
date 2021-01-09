# Commands

PermissionsEx can be configured in-game using its set of commands:

## Argument types

- *subject type*: a type of subject
- *subject*: A compound argument of subject type and identifier
- *context*: A `<context-type>=<context-value>` 
- *rank-ladder*: The name of a currently existing rank ladder
- *permission*: A permission string
- *option*: An option string
- *permission value*: a number, boolean, or `none`, `null`, or `unset`


## Common flags

These flags can be specified after any permissions modification command to control the specific areas permissions are updated in:

- `--transient`: If the data should be stored transiently (i.e. only for the current session)
- `--context <context>`: A context to restrict a permission to, such as a world

## The actual commands

### `/pex|permissionsex`

**Description:** Provides simple information about PermissionsEx.

### `/pex|permissionsex help [command]`

**Description:** Provide help about PermissionsEx commands

**Permission:** `permissionsex.help`

Provides an in-game help viewer for information on PermissionsEx commands

### `/pex|permissionsex debug|d [filter]`

**Description:** Toggle whether all permissions checks are logged to console

**Permission:** `permissionsex.debug`

When enabled, any check executed through PermissionsEx will be logged to console. If the `filter`
regular expression is provided, only permissions, objects, and parent names that match the pattern will be logged.

### `/pex|permissionsex import [source data store]`

**Description:** Import data from another data store

**Permission:** `permissionsex.import`

Import permissions data from a configured backend, or automatically configured conversion provider.
If no data store is specified, a list of available sources will be provided.

This import will fully replace any subject in the active data store that is also present in the source data store

### `/pex|permissionsex ranking|rank [ladder]`

**Description:** Print the contents of a rank ladder

**Permission:** `permissionsex.rank.view.<ladder>`

Clickable buttons are added to ease working with rank ladders.

### `/pex|permissionsex ranking|rank <ladder> add|+ <subject> [position] [-r|--relative]`

**Description:** Add a subject to the rank ladder

**Permission:** `permissionsex.rank.add.<ladder>`

Adds a subject to the rank ladder, by default at the end of the ladder. If a position is specified,
the rank will be inserted at that position. If the rank is already on the ladder and the `--relative`
flag is provided, the rank will be moved by the specified number of positions.

### `/pex|permissionsex ranking|rank <ladder> remove|rem|- <subject>`

**Description:** Remove a subject from a rank ladder.

**Permission:** `permissionsex.rank.remove.<ladder>`

Remove a subject from a rank ladder.

### `/pex|permissionsex reload|rel`

**Description:** Reload all PermissionsEx configuration

**Permission:** `permissionsex.reload`

Reloads all permissions data. The reload is performed asynchronously, and new data will only be
applied if the reload is successful.

### `/pex|permissionsex version [--verbose|-v]`

**Description**: Provide information on the current PermissionsEx version.

**Permission:** `permissionsex.version`

Print detailed information on the version of PermissionsEx that is currently running. If
the `verbose` option is provided, more detailed information on base directories will be printed.

### `/pex|permissions <type> list`

**Description:** List all subjects of a certain type

**Permission:** `permissionsex.list.<type>`

### `/pex|permissionsex <type> <subject> delete [--transient|-t]`

**Description:**: Delete all data for a subject

**Permission:** `permissionsex.delete.<type>.<subject>`

### `/pex|permissionsex <type> <subject> info`

**Description:**: Print all known information for a certain subject.

**Permission:** `permissionsex.info.<type>.<subject>`

All information will be printed for the subject

### `/pex|permissionsex <type> <subject> option|options|opt|o|meta <key> [value] [--transient|-t] [--context key=value]`

**Description:**: Sets or unsets an option for the subject

**Permission:** `permissionsex.option.set.<type>.<subject>`

Sets (if `value` is provided) or unset (if not provided) a certain option on the subject.

### `/pex|permissionsex <type> <subject> parents|parent|par|p add|a|+ <type> <subject> [--transient|t] [--context key=value]`

**Description:**: Adds a parent to the subject

**Permission:** `permissionsex.parent.add.<type>.<subject>`

The parent will be added at the first position to the subject, meaning it will take priority over other parents.

### `/pex|permissionsex <type> <subject> parents|parent|par|p remove|rem|delete|del|- <type> <subject> [--transient|t] [--context key=value]`

**Description:**: Adds a parent to the subject

**Permission:** `permissionsex.parent.remove.<type>.<subject>`

Remove the specified parent from the subject.

### `/pex|permissionsex <type> <subject> parents|parent|par|p remove|rem|delete|del|- <type> <subject> [--transient|t] [--context key=value...]`

**Description:**: Replace all parents of the subject with one parent.

**Permission:** `permissionsex.parent.set.<type>.<subject>`

Remove all parents from the subject and replace them with the one provided subject.

### `/pex|permissionsex <type> <subject> permission|permissions|perm|perms|p <permission> <value> [--transient|t] [--context key=value...]`

**Description:**: Set the permission for a subject

**Permission:** `permissionsex.permission.set.<type>.<subject>`

Set a permission on the subject.

Permission values can be a true or false value to explicitly set the permission, `none`, `null`, or `unset` to clear 
the permission, or a number to assign a weight to the permission.

- Prefixing a permission with `#` means that its value will only be applicable to a subject and its direct parents.
- Glob syntax will be evaluated for permissions:
  - `permissionsex.{permission,parent}.set` will evaluate to both `permissionsex.permission.set` and `permissionsex.parent.set`
  - `some.permission[a-z]` will match `some.permissiona`, `some.permissionb`, and so on, all the way through until `some.permissionz`
  
### `/pex|permissionsex <type> <subject> permission-default|perms-def|permsdef|pdef|pd|default|def <value> [--transient|t] [--context key=value...]`

**Description:**: Set the fallback permission value for a subject

**Permission:** `permissionsex.permission.set-default.<type>.<subject>`

Set the default permissions value for a subject. This is the result that is returned for a subject 
when no more specific node matches.

This value is roughly equivalent to the `*` permission formerly used, but will not override 
permissions specifically set to `false`.


### `/promote`

### `/demote`
