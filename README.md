1) Installation
Download PermissionsEx.jar and Permissions.jar, or the ZIP tha contains both.
PermissionsEx.jar is the PEX plugin itself.
Permissions.jar provides a compatibility layer with TheYeti's Permissions.
Put both files in your plugins folder and restart your server. 
PEX will automatically deploy configuration files.

2) Initial Configuration
After server restart there are two files in plugins/PermissionsEx
config.yml - Main PEX config.

[code]
permissions:
    backends:
        file:
            file: permissions.yml
    backend: file
    basedir: plugins/PermissionsEx
[/code]

permissions.yml - File backend permissions storage.

Currently PEX provides two backends: file and sql.
File backend is used by default.

You can switch the backend using "permissions.backend" configuration node:
backend: "file"
or
backend: "sql"

2.1) File backend configuration
All permissions are stored in the permissions.yml file (filename is also adjustable - permissions.backends.file param)

Here is example:
[code]
users:
    t3hk0d3:
        group: default
        permissions:
        - permissions.*
        worlds:
            world:
                permissions:
                - test.*
groups:
    default:
        default: true
        permissions:
        - modifyworld.*
    admins:
        inheritance:
        - default
        permissions:
        - example.permission
        - -punish.noobs
        options:
            test:
                test: '100500'
    testers:
        inheritance:
        - admins
        options:
            test:
                test: '9000'
[/code]

I think there is no need for detailed explanation - everything is obvious.

2.2) SQL backend configuration
Here is an SQL Backend configuration example using MySQL:

[code]
permissions:
    basedir: plugins\PermissionsEx
    backend: sql
    backends:
        sql:
            driver: mysql
            uri: mysql://host/databasename
            password: user
            user: password
[/code]

Everything you need is to change host, database name, database user and password. In most cases host is "localhost".
PEX will automatically deploy database tables and initial groups and permissions.
Do not delete initial data if you not 100% sure what you are doing.

For SQLite configuration you just need to set "permissions.backends.sql.driver" to "sqlite",
and change "permissions.backends.sql.uri" to something like "sqlite:databasename".

[code]
permissions:
    basedir: plugins\PermissionsEx
    backend: sql
    backends:
        sql:
            driver: sqlite
            uri: sqlite:databasename
            password: user
            user: password
[/code]

3) Command system

PEX provides a rich command system, which gives you the ability to control almost every aspect of PEX.
Also Help plugin support is implemented. Just type /help PermissionsEx.

Here is the overview of available commands:

/command - description. ("needed.permissions.node")

/pex - Display help. ("permissions.manage")
/pex reload - Reload configuration. ("permissions.manage.reload")

/pex hierarchy - Print complete user/group hierarhy. ("permissions.manage.users")

/pex backend - Print currently using backend. ("permissions.manage.backend")
/pex backend <backend> - Change permission backend on the fly. Use with caution! ("permissions.manage.backend")

/pex dump <backend> <file> - Dump users/groups to selected <backend> format. ("permissions.dump")

/pex users - Print registred user list (alias). ("permissions.manage.users")
/pex users list - Print registred user list. ("permissions.manage.users")

/pex groups - Print registed group list (alias). ("permissions.manage.groups")
/pex groups list - Print registred group list. ("permissions.manage.groups")

/pex user <user> - List all user permissions. ("permissions.manage.users.permissions")
/pex user <user> list - List all user permissions. ("permissions.manage.users.permissions")
/pex user <user> group list - List all user groups. ("permissions.manage.membership")
/pex user <user> prefix [newprefix] - Print current user's prefix. Specify newprefix to set prefix. ("permissions.manage.users")
/pex user <user> suffix [newsuffix] - Print current user's suffix. Specify newsuffix to set suffix. ("permissions.manage.users")
/pex user <user> add <permission> [world] - Add permission to user. ("permissions.manage.users.permissions")
/pex user <user> set <permission> <value> [world] - Set option to given value. ("permissions.manage.users.permissions")
/pex user <user> remove <permission> [world] - Remove permission from user. ("permissions.manage.users.permissions")
/pex user <user> group add <group> - Add user to specified group. ("permissions.manage.membership")
/pex user <user> group set <group> - Set only group for user (remove from others). ("permissions.manage.membership")
/pex user <user> group remove <group> - Remove user from specified group. ("permissions.manage.membership")

/pex group <group> - List all group permissions (alias). ("permissions.manage.groups")
/pex group <group> list - List all group permissions. ("permissions.manage.groups")
/pex group <group> users - List all group user permissions. ("permissions.manage.membership")
/pex group <group> prefix [newprefix] - Print current group's prefix. Specify newprefix to set prefix. ("permissions.manage.groups")
/pex group <group> suffix [newsuffix] - Print current group's suffix. Specify newsuffix to set suffix. ("permissions.manage.groups")
/pex group <group> create [parents] - Create group, optionally set parent groups by comma-separated list. ("permissions.manage.groups.create")
/pex group <group> delete - Removes group. ("permissions.manage.groups.remove")
/pex group <group> parents - List all group parents (alias). ("permissions.manage.groups.inheritance")
/pex group <group> parents list - List all group parents. ("permissions.manage.groups.inheritance")
/pex group <group> parents set <parents> - Set parents by comma-separated list. ("permissions.manage.groups.inheritance")
/pex group <group> add <permission> [world] - Add permission to group. ("permissions.manage.groups.permissions")
/pex group <group> set <permission> <value> [world] - Add permission for group. ("permissions.manage.groups.permissions")
/pex group <group> remove <permission> [world] - Remove permission from group. ("permissions.manage.groups.permissions")
/pex group <group> user add <users> - Add users (one or comma-separated list) to specified group. ("permissions.manage.membership")
/pex group <group> user remove <users> - Add users (one or comma-separated list) to specified group. ("permissions.manage.membership")

4) Compatibility layer
There is no need for additional configuration. Everything should work "out of the box".
Just be sure the bundled Permissions.jar is placed together with PermissionEx.jar in the plugins folder.

5) Embedded "Modifyworld" restriction
Plenty of embedded restriction (antigbuild/antigriefing) mechanisms are embedded in PEX.
Modifyworld is disabled by default. To enable Modifyworld in config.yml set "permissions.modifyworld" to true.

You can manipulate players abilities with the following permissions:

modifyworld.blocks.place.<blockid> - Ability to place blocks
modifyworld.blocks.destroy.<blockid> - Ability to destroy blocks
modifyworld.blocks.interact.<blockid> - Ability to interact with blocks (levers, buttons, or just left and right clicking on blocks)

You can specify which block user can place/destroy/iteract or not, example:
        -modifyworld.blocks.place.46 - Prevent user from placing TNT
        -modifyworld.blocks.destroy.57 - Prevent user from destroying diamond blocks
        modifyworld.blocks.iteract.77 - Allow user interacy only with button (77)
        modifyworld.blocks.place.* - Allow user to place all blocks (except TNT as stated above).
        modifyworld.blocks.destory.* - Allow user to destroy all blocks (except diamond blocks).

modifyworld.items.pickup.<itemid> - Ability to pickup items, you can choose which items same as blocks.
modifyworld.items.drop.<itemid> - Ability to drop items.

modifyworld.chat - Ability to chat.
modifyworld.chat.private - Ability to chat privately (/tell)

modifyworld.bucket.fill - Bucket filling.
modifyworld.bucket.empty - Emptying buckets.

modifyworld.usebeds - Ability to sleep in beds. :)

modifyworld.entity.damage.deal - Ability to deal damage to entities (Mobs, Animals and Players)
modifyworld.entity.damage.take - Ability to take damage from entities
modifyworld.entity.mobtarget - Ability to be targeted by entity (Mob or Animal)

modifyworld.vehicle.enter - Ability to use vehicles (Minecarts, Boats, etc)
modifyworld.vehicle.destroy - Ability to destroy vehicles
modifyworld.vehicle.collide - Ability to collide with vehicles. This sounds silly, but yes you can be ghost now :3

By default all users have modifyworld.* permission, this means what users can do all described above by default.

Also there is ability to inform user when he has not enough permissions. Just set "permissions.verbose" in config.yml to true/false.

6) Permissions check order

Permissions checks the nodes in the same order as TheYeti's Permissions plugin does - up-to-down
Let's look this example:

User has following permissions:

-permission.to.spoil.air
permissions.to.*

This means what user can do everything in permissions.to.* namespace, except spoiling air.
So denying permissions should be above allowing permissions.
Order is important!

Minus (-) before permission statement stands for denial permission.
This means, if checked permission matched such negative node, users action instantly will be refused.
