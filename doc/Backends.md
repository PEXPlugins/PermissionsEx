PermissionsEx offers several backends that can be used. The default is SQL, though file is also provided. The [ExtraBackends plugin] provides other backends that are available with a lower level of support. Any number of backends can be configured in the PermissionsEx configuration file, where the `type` field determines the backend type used (note the backend name has no effect on the type used), and the rest of the fields are managed by the backend type.

# Built-in backends

## File

Type: `file`

Configuration Field   | Default            | Usage
--------------------- | ------------------ | ------
`file`                | `permissions.json` | The file to read from. If this file is a YAML file, it will be converted to json.
`alphabetize-entries` | `false`            | If true, place entries in the file in alphabetical order. Otherwise, maintain insertion order.


## SQL

Type: `sql`

Configuration Field | Default | Usage
------------------- | ------- | ------
`url`               | none    | The database connection URL, in the format `<prefix>:[//][user:password]@host/database`. For advanced users, additional parameters are accepted as they would in any JDBC connection string.
`prefix`            | `pex`   | The table prefix to use in the database, to allow PEX to share a database with other plugins and not have table conflicts.


# Extra Backends

All the backends from now on are provided by the ExtraBackends plugin, and will not work without it present.

## GroupManager

This backend allows read-only access to GroupManager data. Once this backend is configured, its data can be imported into the new PEX backend.

Type: `groupmanager`

Configuration Field   | Default                | Usage
--------------------- | ---------------------- | ------
`group-manager-root`  | `plugins/GroupManager` | The folder to look for GroupManager-format permissions data in.

# Changing Backends

PermissionsEx supports importing data into one backend from another. When changing backends, make sure to have both the old backend and the new backend present in the configuration. The new backend should be selected as the default backend. Then, simply run `/pex import <old backend name>` and wait to be notified of completion. This job will execute asynchronously, so the server should remain functional while the import is in progress. To avoid users logging in while the import is in progress, whitelisting could be enabled.


[ExtraBackends plugin]: /PEXPlugins/ExtraBackends
