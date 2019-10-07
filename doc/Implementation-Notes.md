Because PermissionsEx can run in a variety of environments, different implementations of PEX behave somewhat differently.


## Sponge

- Contexts provided by SpongeAPI are currently not included -- they must be registered directly in PEX

## Velocity Proxy

- All commands have an extra `/` prefix to be run on the proxy, and output gold text to distinguish them from commands running on the server. 
  For example, `/pex` will become `//pex` to work with the proxy's instance of the plugin.
- All subjects, when queried on the proxy, are in the `proxy=true` context

## Waterfall/Bungee Proxy

- All commands have an extra `/` prefix to be run on the proxy, and output gold text to distinguish them from commands running on the server. 
  For example, `/pex` will become `//pex` to work with the proxy's instance of the plugin.
- All subjects, when queried on the proxy, are in the `proxy=true` context

## Fabric

### Permission check methods

- ServerCommandSource.hasPermissionLevel
- Entity.allowsPermissionLevel
- Entity.getPermissionLevel
- PlayerEntity.isCreativeLevelTwoOp
- ServerPlayNetworkHandler.isServerOwner
- MinecraftServer.isOwner
- MinecraftServer.getPermissionLevel
- PlayerManager.isOperator
- PlayerManager.areCheatsAllowed
- PlayerManager.getOpList
- PlayerManager.getOpNames
- OperatorList.get


### Types of subjects
- Rcon
- All entities ?!?
- Command blocks

### Known outstanding issues
- Broadcast to ops should have the subject type of the source in the permission somehow
- Check whether we enforce creative mode for certain items (debug stick, etc)

### Client checks op for
- F3 Debug toggle creative/spectator 