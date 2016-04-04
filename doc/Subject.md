Everything in PermissionsEx is based around the subject. A subject is a user, group, server console, irc bot, or any other object that has permissions access. Each subject is composed of *segments* which each have a set of permissions, options, and inheritance that are active when the segment's set of parameters matches.


# Permissions

Permissions exist in a tree structure where each node has a weighting. A permission is split by the `.` character, and each segment can be set to a value. 

*TODO*

# Options

Options are a simple key-value mapping. When multiple locations

# Parents

Any subject can inherit from any other subject.

# Contexts

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



