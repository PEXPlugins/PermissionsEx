-- PermissionsEx Schema v3, H2 Edition
-- Data Types Used
-- ---------------
-- Identifier: varchar(255)
-- Permission value: smallint
-- Unique ids: int(11)


CREATE TABLE `subjects` (
  `id` int(11) NOT NULL IDENTITY,
  `type` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  UNIQUE (`type`,`name`),
);
CREATE INDEX ON `subjects` (`type`);

CREATE TABLE `segments` (
  `id` int(11) NOT NULL IDENTITY ,
  `subject` int(11) NOT NULL,
  `perm_default` smallint DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`subject`) REFERENCES `subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX ON `segments` (`subject`);

CREATE TABLE `permissions` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) DEFAULT NULL,
  `value` smallint DEFAULT NULL,
  UNIQUE (`segment`,`key`),
  FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `contexts` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  UNIQUE (`segment`,`key`),
  UNIQUE (`segment`,`key`,`value`),
  FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `options` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) DEFAULT NULL,
  `value` text,
  UNIQUE (`segment`,`key`),
  FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `inheritance` (
  `segment` int(11) NOT NULL,
  `parent` int(11) NOT NULL,
  UNIQUE (`segment`,`parent`),
  FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (`parent`) REFERENCES `subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX ON `inheritance`(`parent`);

CREATE TABLE `rank_ladders` (
  `id` int(11) NOT NULL IDENTITY ,
  `name` varchar(255) DEFAULT NULL,
  `idx` int(11) DEFAULT NULL,
  `subject` int(11) DEFAULT NULL,
  UNIQUE KEY `name` (`name`,`idx`,`subject`),
  KEY `subject` (`subject`),
  CONSTRAINT `rank_ladders_ibfk_1` FOREIGN KEY (`subject`) REFERENCES `subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX

CREATE TABLE `context_inheritance` (
  `id` int(11) NOT NULL IDENTITY,
  `child_key` varchar(255) DEFAULT NULL,
  `child_value` varchar(255) DEFAULT NULL,
  `parent_key` varchar(255) DEFAULT NULL,
  `parent_value` varchar(255) DEFAULT NULL,
  UNIQUE KEY `child_key_2` (`child_key`,`child_value`,`parent_key`,`parent_value`),
  KEY `child_key` (`child_key`,`child_value`),
  KEY `parent_key` (`parent_key`,`parent_value`)
);
