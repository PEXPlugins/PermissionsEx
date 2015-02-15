CREATE TABLE IF NOT EXISTS `{pex_groups}` (
  `id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name` TEXT NOT NULL,
);

CREATE TABLE IF NOT EXISTS `{pex_qualifiers}` (
  `id` integer PRIMARY KEY NOT NULL AUTOINCREMENT,
  `group` integer NOT NULL REFERENCES `{pex_groups}` (`id`) ON DELETE CASCADE,
  `key` TEXT NOT NULL,
  `value` TEXT NOT NULL,
);

CREATE TABLE IF NOT EXISTS `{pex_entries}` (
  `id` integer PRIMARY KEY NOT NULL AUTOINCREMENT,
  `group` integer NOT NULL REFERENCES `{pex_groups}` (`id`) ON DELETE CASCADE,
  `key` TEXT NOT NULL,
  `value` TEXT NULL,
);

/*CREATE TABLE `{permissions}` (
  `id`          integer PRIMARY KEY AUTOINCREMENT,
  `name`        varchar(50) NOT NULL,
  `type`        int NOT NULL DEFAULT 0,
  `permission`  TEXT NOT NULL,
  `world`       varchar(50) NOT NULL,
  `value`       TEXT NOT NULL
);

CREATE INDEX `permissions_Index01` ON `{permissions}` (`name`, `type`, `world`, `permission`);
CREATE INDEX `permissions_Index02` ON `{permissions}` (`name`, `type`, `world`);

CREATE TABLE `{permissions_entity}` (
  `id`         integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name`       varchar(50) NOT NULL,
  `type`       int NOT NULL DEFAULT 0,
  `default`   int NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX `permissions_entity_Index01` ON `{permissions_entity}` (`name`, `type`);

CREATE TABLE `{permissions_inheritance}` (
  `id`      integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  `child`   varchar(50) NOT NULL,
  `parent`  varchar(50) NOT NULL,
  `type`    int NOT NULL,
  `world`  varchar(50) NULL
);

CREATE INDEX `permissions_inheritance_Index01` ON `{permissions_inheritance}` (`child`, `type`, `world`);

CREATE INDEX `permissions_inheritance_Index02` ON `{permissions_inheritance}` (`parent`, `type`);*/
