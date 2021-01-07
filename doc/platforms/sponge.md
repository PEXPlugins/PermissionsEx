# PermissionsEx for Sponge

PermissionsEx integrates directly with the Sponge permissions API. This API was designed together with PEX v2, so they have fairly equivalent functionality. PEX itself exposes a few more features than Sponge does, so it may be necessary for developers to integrate directly with the PEX API.

If another permissions plugin is installed, PEX will fail to enable.

## Contexts

In addition to the [global contexts](../components-in-detail/subject.md#contexts)

| Context Key | Example Value | Description |
| :--- | :--- | :--- |
| `world` | `DIM-1` | The world a subject is currently in |
| `dimension` | `Overworld` | The dimension of the world a subject is currently in |
| `remoteip` | `127.0.0.1` | The IP address a subject is connecting from |
| `localhost` | `myminecraftserver.com` | The hostname a client is connecting to the server with |
| `localip` | `[2607:f8b0:400a:801::200e]` | The ip \(on the server\) that is receiving the connection from a subject |
| `localport` | `25565` | The port \(on the server\) that the client is connecting to |
