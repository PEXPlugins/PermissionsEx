Everything in PermissionsEx is based around the subject. A subject is a user, group, server console, irc bot, or any other object that has permissions access. Each subject is composed of *segments* which each have a set of permissions, options, and inheritance that are active when the segment's set of parameters matches.

# Segment Parameters
A subject is composed of multiple segments. There can be only one segment with a certain set of parameters. PEX will attempt to clean up duplicates on load, if necessary.

The parameters a segment supports are:

- Inheritable
- Weight
- Contexts

## Contexts

Contexts are tags that can be applied to a segment that restrict the segment's applicability to a certain player state. Sponge provides several contexts and PermissionsEx applies a few more, but any plugin can add its own contexts (for example a region protection plugin could add a `region` context to allow for region-specific permissions). When PermissionsEx calculates permissions for a set of active contexts, any segment whose set of contexts is a subset of the subject's active contexts will be considered.

**Sponge contexts**

Context Key | Example Value              | Description
----------- | -------------------------- | -----------
`world`     | `DIM-1`                    | The world a subject is currently in
`dimension` | `Overworld`                | The dimension of the world a subject is currently in
`remoteip`  | `127.0.0.1`                | The IP address a subject is connecting from
`localhost` | `myminecraftserver.com`    | The hostname a client is connecting to the server with
`localip`   | `2607:f8b0:400a:801::200e` | The ip (on the server) that is receiving the connection from a subject
`localport` | `25565`                    | The port (on the server) that the client is connecting to

**PermissionsEx Contexts**

Context Key | Example Value              | Description
----------- | -------------------------- | -----------
`server-tag`| `creative`                 | Tags applied to the current server (in `permissionsex.conf`)

# Permissions

Permissions exist in a tree structure where each node has a value. A permission is split by the `.` character, and each segment can be set to a value. 

When resolving permissions, the resolution picks the value from highest to lowest weight, then at the same weight from most to least specific, then at the same specificity, whatever's nearest in the inheritance hierarchy.

Shell globs can be used to specify multiple permissions at once. For example, the glob `worldedit.navigation.{jumpto,thru}.tool` would give both `worldedit.navigation.jumpto.tool` and `worldedit.navigation.thru.tool`. These can be nested if necessary, and help to make easier

## Commands

`/pex <type> <identifier> permission|perm <permission> <value>`


# Permission Default

Now that we've seen the tree structure of permissions nodes, there has to be some value at the root of a tree, the value returned for permissions that haven't been set. This is what the default permission is. This is the replacement for the `*` node of PEX 1.x.

## Commands

`/pex <type> <identifier> def <value>`

# Options

Options are a simple key-value mapping, using the permissions resolution strategy (except that dots have no special meaning here). These can be used, for example, in chat plugins or world protection plugins where attributes like `prefix` or `build` need to be stored and possibly inherited.

## Commands

`/pex <type> <identifier> option|opt <key> [value]`

# Parents

Any subject can inherit from any other subject. When a subject has a parent, it inherits any data set on that parent, at a lower priority than any data set on itself.

## Commands

- `/pex <type> <identifier> parent add [parent-type (defaults to group)] <parent-identifier>`
- `/pex <type> <identifier> parent remove [parent-type (defaults to group)] <parent-identifier>`





