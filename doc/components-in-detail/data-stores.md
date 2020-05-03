# Data Stores

PermissionsEx can store its data in a variety of formats. By default, PEX uses the `file` backend to store permissions in the `permissions.json` file in the plugin data folder, but several others are available

## Moving between data stores

The currently active data store is set with the `default-backend` option in the plugin's configuration file. The default backend may be chosen from any data store configured in the `backends` map in the configuration. While only two data stores are listed by default, any number may be configured -- PermissionsEx only loads the one specified in `default-backend` and whichever data stores may be requested for imports.

The command `\pex import [id]` will import a data store with the id `id`, or if no parameters are provided give a list of available data stores to import from.

When importing from another data store, any subject that is present in the data store being imported from will **completely overwrite** the subject with the same identifier in the destination \(i.e. currently active\) data store.

## Data Store Types

### JSON File `file`

The default backend, writing to a file in the PermissionsEx data folder

#### Options

| Option | Purpose | Default Value |
| :--- | :--- | :--- |
| `file` | The file to use | `permissions.json` |
| `auto-reload` | Automatically reload permissions when a change is made to the file | `true` |
| `alphabetize-entries` | Whether to sort entries alphabetically \(when true\), or maintain existing order \(when false\) | `false` |

### `sql`

Stores permissions data in an SQL database.

Currently supported databases are H2 and MariaDB/MySQL. PEX uses server-provided database connectors, so different platforms may have more limited support.

| Option | Purpose | Default Value |
| :--- | :--- | :--- |
| `url` | The URL of the database to connect to, in the format `jdbc:<type>:[/[[<user>:<password>@]<host>/]<database>`, where brackets indicate parameters | `h2:permissions.db` |

## Migration \(read-only\) data stores

These data stores are written to migrate from other permissions plugins. Generally these do not have to be configured manually -- they will be detected and made available for import when running `\pex import`

* LuckPerms \(supports combined file format only for the moment, and does not support TOML\)
* UltraPermissions \(not yet implemented\)
* Ops file
* GroupManager

