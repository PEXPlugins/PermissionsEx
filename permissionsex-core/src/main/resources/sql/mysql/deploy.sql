-- PermissionsEx Schema v3, MySQL/MariaDB Edition
-- Requires InnoDB backend for foreign keys

-- Data Types Used
-- ---------------
-- Identifier: varchar(255)
-- Permission value: smallint
-- Unique ids: int(11)

CREATE TABLE `{}subjects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ident` (`type`,`name`),
  KEY `type_k` (`type`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}segments` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `subject` int(11) NOT NULL,
  `perm_default` smallint(6) DEFAULT NULL,
  KEY `subject_k` (`subject`),
  CONSTRAINT `subject_fk` FOREIGN KEY (`subject`) REFERENCES `{}subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}permissions` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` smallint(6) NOT NULL,
  UNIQUE KEY `segment_k` (`segment`,`key`),
  CONSTRAINT `segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

 CREATE TABLE `{}contexts` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  UNIQUE KEY `k` (`segment`,`key`),
  UNIQUE KEY `kv` (`segment`,`key`,`value`),
  CONSTRAINT `segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}options` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` text,
  UNIQUE KEY `segment` (`segment`,`key`),
  CONSTRAINT `segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}inheritance` (
  `segment` int(11) NOT NULL,
  `parent` int(11) NOT NULL,
  UNIQUE KEY `segment` (`segment`,`parent`),
  KEY `parent` (`parent`),
  CONSTRAINT `segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `parent_fk` FOREIGN KEY (`parent`) REFERENCES `{}subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

 CREATE TABLE `{}rank_ladders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `idx` int(11) NOT NULL,
  `subject` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`,`idx`,`subject`),
  KEY `subject` (`subject`),
  CONSTRAINT `subject_fk` FOREIGN KEY (`subject`) REFERENCES `{}subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

 CREATE TABLE `{}context_inheritance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `child_key` varchar(255) NOT NULL,
  `child_value` varchar(255) NOT NULL,
  `parent_key` varchar(255) NOT NULL,
  `parent_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `both_key` (`child_key`,`child_value`,`parent_key`,`parent_value`),
  KEY `child_key` (`child_key`,`child_value`),
  KEY `parent_key` (`parent_key`,`parent_value`)
) DEFAULT CHARSET=utf8;
