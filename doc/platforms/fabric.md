# PermissionsEx for Fabric

On Fabric, the only system provided is Minecraft's op permissions system. Because of that, PermissionsEx provides all of its own integrations with Minecraft itself, and some other mods. We are happy to work with other mods to implement permissions checks as well -- feel free to pop into our Discord.

## Minecraft permissions

We aim to replace every op check in vanilla \(i.e. unmodded\) Minecraft with a permissions check. To that end, we provide the following permissions:

| Permission | Purpose |
| :--- | :--- |
| `minecraft.command.<command>` | Use the command `command`. Commands available to all users in vanilla should be granted by default. |
| `minecraft.bypass.whitelist` | Allow users to log in to a whitelisted server without being whitelisted |
| `minecraft.bypass.spawnprotection` | Allow users to build within the spawn protection radius |
| `minecraft.bypass.playercount` | Allow users to log in even when the server has reached its player limit |
| `minecraft.bypass.movespeed.player` | Bypass the "Player moved too fast" limit |
| `minecraft.bypass.movespeed.vehicle` | Bypass the "Player moved too fast" kick when in a vehicle |
| `minecraft.bypass.chatspeed` | Bypass the kick for sending chat messages faster than allowed by the game |
| `minecraft.updatedifficulty` | Allow setting the game's difficulty |
| `minecraft.<commandblock/jigsawblock/structureblock>.<view/edit/break>` | Allow placing/editing/viewing/breaking a command block, jigsaw block, or structure block depending on which permissions are given |
| `minecraft.debugstick.use.<target>` | Allow using the debug stick on the `target`ed block. Targets are specified as `<namespace>.<item>` \(e.g. `minecraft.stone`\). Some functionality may require being in creative mode due to client limitations. |
| `minecraft.nbt.<query/load>.<entity/block>` | Allow placing blocks or entities while preserving their NBT data \(`BlockEntityTag` and such\) |
| `minecraft.selector` | Allow using selectors in commands |
| `minecraft.adminbroadcast.send` | Allow sending admin broadcasts to other players |
| `minecraft.adminbroadcast.receive` | Receive messages sent to "ops" from command output |

## Subjects for Minecraft entities

* Players are represented as `user`s
* The server console is represented as the subject `system:Console`
* Command blocks are represented as `commandblock:<name>`, where the default command block name is `@`
* Sign representation has not been decided yet
* RCON connections are represented as `system:Recon` \(Yes, that is `Recon` -- that's what the Rcon command source is named internally.\)
* Functions executed through tags `#minecraft:tick` and `#minecraft:load` tags are represented as `function:minecraft:tick` and `function:minecraft:load` respectively

NOTE: functions currently only perform permissions checks when they're loaded, not on individual executions. This needs to be resolved for function permissions to actually be useful.

## Contexts

| Context Key | Example Value | Description |
| :--- | :--- | :--- |
| `world` | `minecraft:the_nether` | The identifier of a dimension a subject is currently in |
| `dimension_type` | `minecraft:the_nether` | The identifier of the type of dimension a subject is in |
| `remoteip` | `127.0.0.1` | The IP address a subject is connecting from |
| `localhost` | `myminecraftserver.com` | The hostname a client is connecting to the server with |
| `localip` | `[2607:f8b0:400a:801::200e]` | The ip \(on the server\) that is receiving the connection from a subject |
| `localport` | `25565` | The port \(on the server\) that the client is connecting to |
| `function` | `permissionsex:test_function` | The function currently executing. When nested functions are in progress, multiple values will be specified. |


## Other mods

* We provide a permission resolver for [WorldEdit](https://enginehub.org/worldedit) so its commands check PermissionsEx for permissions

## Developer information

PEX provides a significant level of integration into Fabric. This provides some details on what we look for when trying to provide permissions checks

### Permission check methods

* ServerCommandSource.hasPermissionLevel
* Entity.allowsPermissionLevel
* Entity.getPermissionLevel
* PlayerEntity.isCreativeLevelTwoOp
* ServerPlayNetworkHandler.isServerOwner
* MinecraftServer.isOwner
* MinecraftServer.getPermissionLevel
* PlayerManager.isOperator
* PlayerManager.areCheatsAllowed
* PlayerManager.getOpList
* PlayerManager.getOpNames
* OperatorList.get

### Known outstanding issues

* Broadcast to ops should have the subject type of the source in the permission somehow
* Check whether we enforce creative mode for certain items \(debug stick, etc\)
* Changing permissions is very slow -- investigate this

### Client checks op for

* F3 Debug toggle creative/spectator
* Debug stick and other technical blocks
