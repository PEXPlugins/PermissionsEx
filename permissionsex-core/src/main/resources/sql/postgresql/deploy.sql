CREATE TABLE `subjects` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`type` varchar(255) DEFAULT NULL,
`name` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `ident` (`type`,`name`),
KEY `type_k` (`type`)
) AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

CREATE TABLE `segments` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`subject` int(11) NOT NULL,
`perm_default` smallint(6) DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `subject` (`subject`),
CONSTRAINT `segments_ibfk_1` FOREIGN KEY (`subject`) REFERENCES `subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

CREATE TABLE `permissions` (
`segment` int(11) NOT NULL,
`key` varchar(255) DEFAULT NULL,
`value` smallint(6) DEFAULT NULL,
UNIQUE KEY `segment` (`segment`,`key`),
CONSTRAINT `permissions_ibfk_1` FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `contexts` (
`segment` int(11) NOT NULL,
`key` varchar(255) NOT NULL,
`value` varchar(255) NOT NULL,
UNIQUE KEY `k` (`segment`,`key`),
UNIQUE KEY `kv` (`segment`,`key`,`value`),
CONSTRAINT `contexts_ibfk_1` FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `options` (
`segment` int(11) NOT NULL,
`key` varchar(255) DEFAULT NULL,
`value` text,
UNIQUE KEY `segment` (`segment`,`key`),
CONSTRAINT `options_ibfk_1` FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `inheritance` (
`segment` int(11) NOT NULL,
`parent` int(11) NOT NULL,
UNIQUE KEY `segment` (`segment`,`parent`),
KEY `parent` (`parent`),
CONSTRAINT `inheritance_ibfk_1` FOREIGN KEY (`segment`) REFERENCES `segments` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
CONSTRAINT `inheritance_ibfk_2` FOREIGN KEY (`parent`) REFERENCES `subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `rank_ladders` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(255) DEFAULT NULL,
`idx` int(11) DEFAULT NULL,
`subject` int(11) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `name` (`name`,`idx`,`subject`),
KEY `subject` (`subject`),
CONSTRAINT `rank_ladders_ibfk_1` FOREIGN KEY (`subject`) REFERENCES `subjects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) DEFAULT CHARSET=utf8;

CREATE TABLE `context_inheritance` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`child_key` varchar(255) DEFAULT NULL,
`child_value` varchar(255) DEFAULT NULL,
`parent_key` varchar(255) DEFAULT NULL,
`parent_value` varchar(255) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `child_key_2` (`child_key`,`child_value`,`parent_key`,`parent_value`),
KEY `child_key` (`child_key`,`child_value`),
KEY `parent_key` (`parent_key`,`parent_value`)
) DEFAULT CHARSET=utf8;
