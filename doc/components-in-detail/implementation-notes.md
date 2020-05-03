# Implementations

Because PermissionsEx can run in a variety of environments, different implementations of PEX behave somewhat differently.

## Paper/Spigot/Bukkit

* We override the Superperms system to follow our own matching rules. Apart from specific changes to match in a more pex-friendly way, we attempt to match standard Bukkit permission resolution as closely as possible \(in terms of following permission parents, op/not op defaults, etc\).
* To expose PEX information to plugins that only use superperms, several metapermissions are provided. These are:

  | Permission | Usage |
  | :--- | :--- |
  | `group.<group>` | Added with the name of each group the player is in, including inherited groups. |
  | `groups.<group>` | same as above |
  | `options.<option>.<value>` | Added for each option set on the player, including options inherited from parents. |
  | `prefix.<prefix>` | The player's prefix. |
  | `suffix.<suffix>` | The player's suffix. |

  Note: these permissions are not exposed through Vault or PEX's API -- both provide better ways to access the same data.

### Vault notes

Vault is the standard permissions API to allow more extensive permissions operations on Bukkit. While Vault provides more functionality than the standard Superperms API, its model of permissions does not match PermissionsEx's entirely.

* Vault permission queries in a world that is not the player's active world do not provide accurate contexts. Because Vault only provides a world field, we only replace the `world` context in the player's active contexts. Any other location-based contexts, such as those for the current dimension or for any active regions, are not updated.
* Permissions and options can only be set in the global context or the context for just one world. Setting data does not use active contexts.

### Plugin integrations

PermissionsEx integrates with some plugins to provide additional permissions features. While the goal is for each of these plugins to bundle their own integrations, during PEX development many of these are included in PEX itself.

| Plugin | Integration | Source |
| :--- | :--- | :--- |
| WorldGuard | `region` context for every region in the player's applicable region set | PEX |

## Sponge

PermissionsEx integrates directly with the Sponge permissions API. This API was designed together with PEX v2, so they have fairly equivalent functionality. PEX itself exposes a few more features than Sponge does, so it may be necessary for developers to integrate directly with the PEX API.

If another permissions plugin is installed, PEX will fail to enable.

## Velocity Proxy

* All commands have an extra `/` prefix to be run on the proxy, and output gold text to distinguish them from commands running on the server. 

  For example, `/pex` will become `//pex` to work with the proxy's instance of the plugin.

* All subjects, when queried on the proxy, are in the `proxy=true` context

## Waterfall/Bungee Proxy

* All commands have an extra `/` prefix to be run on the proxy, and output gold text to distinguish them from commands running on the server. 

  For example, `/pex` will become `//pex` to work with the proxy's instance of the plugin.

* All subjects, when queried on the proxy, are in the `proxy=true` context

## Fabric

On Fabric, the only system provided is Minecraft's op permissions system. Because of that, PermissionsEx provides all of its own integrations with Minecraft itself, and some other mods. We are happy to work with other mods to implement permissions checks as well -- feel free to pop into our Discord.

### Minecraft permissions

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

### Subjects for Minecraft entities

* Players are represented as `user`s
* The server console is represented as the subject `system:Console` 
* Command blocks are represented as `commandblock:<name>`, where the default command block name is `@`
* Sign representation has not been decided yet
* RCON connections are represented as `system:Recon` \(Yes, that is `Recon` -- that's what the Rcon command source is named internally.\)

### Other mods

* We provide a permissions resolver for [WorldEdit](https://enginehub.org/worldedit) so its commands check PermissionsEx for permissions

### Developer information

PEX provides a significant level of integration into Fabric. This provides some details on what we look for when trying to provide permissions checks

#### Permission check methods

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

#### Known outstanding issues

* Broadcast to ops should have the subject type of the source in the permission somehow
* Check whether we enforce creative mode for certain items \(debug stick, etc\)
* Changing permissions is very slow -- investigate this

#### Client checks op for

* F3 Debug toggle creative/spectator 

