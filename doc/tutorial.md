# Getting Started

While PermissionsEx \(PEX\) is quite powerful, its flexibility means that for a beginner there isn't an obvious path to go down. This document aims to provide that path to a basic permissions configuration that most servers can work with or build on further. This guide is primarily targeted towards users new to PEX, and somewhat towards users only new to 2.0. More advanced users may want to navigate directly towards feature-specific information pages.

## Installation

Downloads of dev builds are available from [the CI server](https://jenkins.addstar.com.au/view/PermissionsEx/job/PermissionsEx/). There have not yet been any releases of the 2.x branch as several major features present in 1.x are still in development, so it is important to keep up to date with dev builds. On Sponge, the downloaded jar goes into the `mods/` folder, and on Spigot into the `plugins/` folder. On first launch, configurations will be created in the `config/permissionsex` folder for Sponge, Fabric, and Velocity, and `plugins/PermissionsEx` for Paper/Spigot/Bukkit and Bungee. 1.x configurations _will_ be automatically converted on first run, and users will be converted to UUID.

## First Run

On PEX's first run, the necessary configuration files will be created. By default, no permissions are explicitly assigned to anybody. However, the default configuration gives all permissions to users connecting from the same host as the server \(so if you're running a local server you'll automatically be able to run anything from ingame\). On Sponge, PEX replaces the op system entirely, so setting op beforehand will not have any effect on permissions once PEX is installed.

Currently, the default [data store](components-in-detail/data-stores.md) is the file backend, to be changed to H2 in the future once such a database is available. While the permissions database is human-readable, it is recommended to edit the PEX configuration using in-game commands. The SQL backend for MySQL and PostgreSQL is also available \(**TODO document this**\) for more complex server configurations.

## Creating an admin group

Subjects in PEX are automatically created as soon as data is assigned to them. Because of that, any command that edits a group will create a group if no such group is present, so no command to explicitly create a group exists.

The feature we're going to use for creating an admin group is _permission defaults_. This sets the permission result that will be given when any permission that has not otherwise been specified is checked. This means, that for an admin group, unless a plugin or the user explicitly forbids a permission any user in the group will have the permission. The resulting group will have all permissions, and users can be added. **WARNING** Don't set the permission default to false on the global default subject \(default:default\). This will override subject-specific options, including the console, in effect locking the console out of commands, which is often not the desired behavior.

Currently, a user must have joined and left the server at least once to have identification information stored for PEX to use. However, once that has happened, the following commands \(run from the console or ingame with appropriate permissions\) will create an admin group, creatively named `admin` in this case, though any name is possible as long as it's used consistently.

```text
/pex group admin def true
/pex user <name> parent add group admin
```

Note that the admin group has not been added to any inheritance hierarchies. The admin group does not need any extra permissions, so having it be part of an inheritance hierarchy opens up the possibility for problematic overrides happening. From now on, any permissions changes can be done from ingame if desired.

## Creating general user groups

Beyond the admin group, there are almost as many possibilities for configuring ranks as there are individual servers. For the purposes of this tutorial, we'll create a default group and several inheriting groups as a useful starting point. For any individual server, it may make sense to add or remove groups as necessary. This configuration has the groups connected both by inheritance and in a rank ladder. A rank ladder is used only for the `/promote` and `/demote` commands, while inheritance is only used for permissions and options calculations. Again, because groups are only created when relevant data is present, these commands are just about configuring inheritance.

Our configuration will consist of 3 groups: default, a group users are assigned to on joining the server, member, and VIP. \(All group names are case-sensitive, so make sure you keep that consistent, or use tab completion to verify\)

First off, we add the default group:

```text
/pex default user parent add group default
/pex rank default add group default
```

Then we add the Members group:

```text
/pex rank default add group member
```

Note that we aren't adding the Default group as a parent here. This is because in PEX 2.0, the default group is automatically inserted as a parent in every permissions query, meaning that adding it as a parent to _any_ group would be redundant.

And now the VIP group:

```text
/pex group VIP parent add group member
/pex rank default add group VIP
```

The VIP rank is now created, at the top of the default rank ladder and inheriting data from the Member and Default groups.

This sequence can be repeated to add further groups to the sequence. At any time, to add a user to a specific group it is possible to do so:

```text
/pex user <name> parent add group <group>
```

or to remove a user from a group:

```text
/pex user <name> parent remove group <group>
```

However, these groups don't do much. To fix that, we need to set some permissions and options!

## Adding Permissions and options

This is what PEX is all about: permissions. Now that we have a structure, we can add permissions to each group. Because each plugin has its own set of checked permissions and options, the exact permissions added may vary a lot. However, any permissions changes follow the same structure.

First, let's add prefixes to each group. **Note that PEX does not handle prefixes or suffixes itself.** PEX is purely a data provider plugin, so a separate chat plugin is required for prefixes or suffixes to have any effect. Usually these plugins will use the `prefix` and `suffix` options for prefix and suffix respectively. These are generic options -- PEX doesn't perform any special handling for them, so any chat plugin can choose to do things differently. These options can be set \(as one example\) like so:

```text
/pex group default option prefix "[<color name='grey'>Guest</color>]"
/pex group member option prefix "[<color name='purple'>Member</color>]"
/pex group VIP option prefix "[<color name='gold'>VIP</color>]"
```

Now, add permissions to individual groups. For example, allow VIPs to use the `/jumpto` command and tool, give the following permission:

```text
/pex group VIP permission worldedit.navigation.jumpto true
```

This can be repeated for any other permission as necessary. Permissions can even be grouped together, like such:

```text
/pex group VIP permission featherchat.channel.{add,remove} true
```

This would give both the permissions featherchat.channel.add and featherchat.channel.remove to the group VIP.

Wildcards are another supported permission resolution shortcut. Giving a permission `a` will give any permission that is a subnode of `a` \(split by the `.` character\), for example `a.b`, `a.c`, or `a.b.d`. Resolution is performed with priority as higher weight overrides lower weight, and at the same weight more specific overrides less specific, and at the same specificity and weight closer in inheritance overrides farther in inheritance.

## Further reading

* Rank Ladders
* Timed Permissions
* Contexts

