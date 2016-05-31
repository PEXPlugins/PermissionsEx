-- PermissionsEx Schema v3, MySQL/MariaDB Edition
-- Requires InnoDB engine for foreign keys

-- Data Types Used
-- ---------------
-- Identifier: varchar(255)
-- Permission value: smallint
-- Unique ids: int(11)
-- Text values: text


CREATE TABLE `{}global` (
  `key` VARCHAR(255) PRIMARY KEY,
  `value` TEXT NOT NULL
);

CREATE TABLE `{}subjects` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY ,
  `type` varchar(255) NOT NULL,
  `identifier` varchar(255) NOT NULL,
  UNIQUE KEY `ident` (`type`, `identifier`),
  KEY `type_k` (`type`)
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}segments` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `subject` int(11) NOT NULL,
  `perm_default` TINYINT DEFAULT NULL,
  `weight` int(11) NOT NULL,
  `inheritable` BOOLEAN NOT NULL,
  KEY `subject_k` (`subject`),
  CONSTRAINT `subject_fk` FOREIGN KEY (`subject`) REFERENCES `{}subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}permissions` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` TINYINT NOT NULL,
  UNIQUE KEY `perm_segment_k` (`segment`,`key`),
  CONSTRAINT `perm_segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

 CREATE TABLE `{}contexts` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  UNIQUE KEY `context_k` (`segment`,`key`),
  UNIQUE KEY `context_kv` (`segment`,`key`,`value`),
  CONSTRAINT `context_segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}options` (
  `segment` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` text,
  UNIQUE KEY `option_segment` (`segment`,`key`),
  CONSTRAINT `option_segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `{}inheritance` (
  `segment` int(11) NOT NULL,
  `parent` int(11) NOT NULL,
  UNIQUE KEY `segment` (`segment`,`parent`),
  KEY `parent` (`parent`),
  CONSTRAINT `inheritance_segment_fk` FOREIGN KEY (`segment`) REFERENCES `{}segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `parent_fk` FOREIGN KEY (`parent`) REFERENCES `{}subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

 CREATE TABLE `{}rank_ladders` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(255) NOT NULL,
  `subject` int(11) NOT NULL,
  UNIQUE KEY `key` (`name`, `subject`),
  KEY `ident` (`name`),
  CONSTRAINT `rank_subject_fk` FOREIGN KEY (`subject`) REFERENCES `{}subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

 CREATE TABLE `{}context_inheritance` (
  `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `child_key` varchar(255) NOT NULL,
  `child_value` varchar(255) NOT NULL,
  `parent_key` varchar(255) NOT NULL,
  `parent_value` varchar(255) NOT NULL,
  UNIQUE KEY `both_key` (`child_key`,`child_value`,`parent_key`,`parent_value`),
  KEY `child_key` (`child_key`,`child_value`),
  KEY `parent_key` (`parent_key`,`parent_value`)
) DEFAULT CHARSET=utf8;
