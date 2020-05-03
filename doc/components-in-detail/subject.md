# Subject

Everything in PermissionsEx is based around the subject. A subject is a user, group, server console, irc bot, or any other object that has permissions access. Each subject is composed of _segments_ which each have a set of permissions, options, and inheritance that are active when the segment's set of parameters matches.

## Permissions

Permissions exist in a tree structure where each node has a weighting. A permission is split by the `.` character, and each segment can be set to a value.

_TODO_

## Options

Options are a simple key-value mapping. When multiple locations

## Parents

Any subject can inherit from any other subject.

## Contexts

Contexts are tags that can be applied to a segment that restrict the segment's applicability to a certain player state. Sponge provides several contexts and PermissionsEx applies a few more, but any plugin can add its own contexts \(for example a region protection plugin could add a `region` context to allow for region-specific permissions\). When PermissionsEx calculates permissions for a set of active contexts, any segment whose set of contexts is a subset of the subject's active contexts will be considered.

### Sponge contexts

| Context Key | Example Value | Description |
| :--- | :--- | :--- |
| `world` | `DIM-1` | The world a subject is currently in |
| `dimension` | `Overworld` | The dimension of the world a subject is currently in |
| `remoteip` | `127.0.0.1` | The IP address a subject is connecting from |
| `localhost` | `myminecraftserver.com` | The hostname a client is connecting to the server with |
| `localip` | `[2607:f8b0:400a:801::200e]` | The ip \(on the server\) that is receiving the connection from a subject |
| `localport` | `25565` | The port \(on the server\) that the client is connecting to |

### Paper/Spigot/Bukkit contexts

| Context Key | Example Value | Description |
| :--- | :--- | :--- |
| `world` | `world_nether` | The name of the world a subject is currently in |
| `dimension` | `nether` | The name of the dimension a subject is currently in |
| `remoteip` | `127.0.0.1` | The IP address a subject is connecting from |
| `localhost` | `myminecraftserver.com` | The hostname a client is connecting to the server with |
| `localip` | `[2607:f8b0:400a:801::200e]` | The ip \(on the server\) that is receiving the connection from a subject |
| `localport` | `25565` | The port \(on the server\) that the client is connecting to |

### PermissionsEx Contexts

| Context Key | Example Value | Description |
| :--- | :--- | :--- |
| `server-tag` | `creative` | Tags applied to the current server \(in `permissionsex.conf`\) |
| `before-time` | `2020-07-01T08:00:00` | Only active before this time |
| `after-time` | `2008-04-01T00:00:00` | Only active after the provided time |

#### Time formats

When using `before-time` and `after-time`, PermissionsEx supports a variety of formats:

* ISO Date Time: `2011-12-03T10:15:30` or `2011-12-03T10:15:30+01:00` or `2011-12-03T10:15:30+01:00[Europe/Paris]`
* ISO Time Relative to Today: `10:15` or `10:15:30` or `10:15:30+01:00`
* ISO Date: `2011-12-03` `2011-12-03+01:00`
* RFC 1123: `Tue, 3 Jun 2008 11:05:30 GMT`
* PermissionsEx Relative Time Format: `+1d2h-3m` \(more on this later\)
* Seconds since Epoch: `1578779386573`

**PermissionsEx Relative Time Format**

Times in PermissionsEx can be expressed in terms of the current time. The format is as follows:

* A leading sign \(`+` or `-`\) 
* Quantity \(such as `2`\)
* Unit \(such as `m` for minute\)

A relative time must always begin with a sign, however the same sign will be used until a new one is specified before a later quantity.

Here is a table of the current supported units:

* Seconds: `second`, `seconds`, `s`
* Minutes: `minute`, `minutes`, `m`
* Hours: `hour`, `hours`, `h`
* Days: `day`, `days`, `d`
* Weeks: `week`, `weeks`, `w`
* Months: `month`, `months`
* Years: `year`, `years`

For example, to express 3 days from now, the following can be used: `+3d`

2 days, 4 minutes less 16 seconds can be expressed in the following way: `+2d4m-16s`

In this way, administrators will be able to easily specify times relative to the current time.

