# PermissionsEx for Paper/Spigot/Bukkit

PermissionsEx is supported on Paper, Spigot, and Bukkit, at versions 1.8.8, 1.12.2, and latest. 
While other versions may work, we don't actively test on those versions and issues will not be 
prioritized.

* We override the Superperms system to follow our own matching rules. Apart from specific changes to match in a more pex-friendly way, we attempt to match standard Bukkit permission resolution as closely as possible \(in terms of following permission parents, op/not op defaults, etc\).
* To expose PEX information to plugins that only use superperms, several metapermissions are provided. These are:

  | Permission | Usage |
  | :---       | :---  |
  | `group.<group>` | Added with the name of each group the player is in, including inherited groups. |
  | `groups.<group>` | same as above |
  | `options.<option>.<value>` | Added for each option set on the player, including options inherited from parents. |
  | `prefix.<prefix>` | The player's prefix. |
  | `suffix.<suffix>` | The player's suffix. |

  Note: these permissions are not visible through Vault or PEX's API -- both provide better ways to access the same data.

## Contexts

| Context Key | Example Value | Description |
| :--- | :--- | :--- |
| `world` | `world_nether` | The name of the world a subject is currently in |
| `dimension` | `nether` | The name of the dimension a subject is currently in |
| `remoteip` | `127.0.0.1` | The IP address a subject is connecting from |
| `localhost` | `myminecraftserver.com` | The hostname a client is connecting to the server with |
| `localip` | `[2607:f8b0:400a:801::200e]` | The ip \(on the server\) that is receiving the connection from a subject |
| `localport` | `25565` | The port \(on the server\) that the client is connecting to |

### Plugin integrations

PermissionsEx integrates with some plugins to provide additional permissions features. While the 
goal is for each of these plugins to bundle their own integrations, during PEX development many of 
these are included in PEX itself.

| Plugin | Integration | Source |
| :--- | :--- | :--- |
| WorldGuard | `region` context for every region in the player's applicable region set | PEX |

## Vault notes

Vault is the standard permissions API to allow more extensive permissions operations on Bukkit. While Vault provides more functionality than the standard Superperms API, its model of permissions does not match PermissionsEx's entirely.

* Vault permission queries in a world that is not the player's active world do not provide accurate contexts. Because Vault only provides a world field, we only replace the `world` context in the player's active contexts. Any other location-based contexts, such as those for the current dimension or for any active regions, are not updated.
* Permissions and options can only be set in the global context or the context for just one world. Setting data does not use active contexts.

