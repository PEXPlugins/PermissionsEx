CREATE TABLE permissions (
  id          integer PRIMARY KEY AUTOINCREMENT,
  name        varchar(50) NOT NULL,
  type        int NOT NULL DEFAULT 0,
  permission  varchar(200) NOT NULL,
  world       varchar(50) NOT NULL,
  value       varchar(255) NOT NULL
);

CREATE INDEX permissions_Index01
  ON permissions
  (name, type, world, permission);

CREATE INDEX permissions_Index02
  ON permissions
  (name, type, world);

CREATE TABLE permissions_entity (
  id         integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  name       varchar(50) NOT NULL,
  type       int NOT NULL DEFAULT 0,
  prefix     varchar(255) NOT NULL,
  suffix     varchar(255) NOT NULL,
  "default"  int NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX permissions_entity_Index01
  ON permissions_entity
  (name, type);

CREATE TABLE permissions_inheritance (
  id      integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  child   varchar(50) NOT NULL,
  parent  varchar(50) NOT NULL,
  type    int NOT NULL
);

CREATE INDEX permissions_inheritance_Index01
  ON permissions_inheritance
  (child, type);

CREATE INDEX permissions_inheritance_Index02
  ON permissions_inheritance
  (parent, type);

INSERT INTO permissions (id, name, type, permission, world, value) VALUES (1, 'default', 0, 'modifyworld.*', '', '');
INSERT INTO permissions_entity (id, name, type, prefix, suffix, "default") VALUES (2, 'default', 0, '', '', 1);