# Possible 2.0 Schema
CREATE TABLE IF NOT EXISTS `{segments}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(50),
  `identifier` VARCHAR(50),
  UNIQUE KEY `unique` (`type`,`identifier`)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `{permissions}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `segment` int(11) REFERENCES `{segments}` (`id`) ON DELETE CASCADE,
  `key` TEXT,
  `value` int(11)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `{options}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `segment` int(11) REFERENCES `{segments}` (`id`) ON DELETE CASCADE,
  `key` TEXT,
  `value` TEXT
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `{inheritance}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `segment` int(11) REFERENCES `{segments}` (`id`) ON DELETE CASCADE,
  `parent` int(11) REFERENCES `{segments}` (`id`) ON DELETE CASCADE,
  UNIQUE KEY `unique` (`segment`, `parent`)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;


# Current

CREATE TABLE IF NOT EXISTS `{permissions}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `permission` text NOT NULL,
  `world` varchar(50) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`id`),
  #UNIQUE KEY `unique` (`name`,`permission`,`world`,`type`),
  KEY `user` (`name`,`type`),
  KEY `world` (`world`,`name`,`type`)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;


CREATE TABLE IF NOT EXISTS `{permissions_entity}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `default` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`, `type`),
  KEY `default` (`default`)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `{permissions_inheritance}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `child` varchar(50) NOT NULL,
  `parent` varchar(50) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `world` varchar(50) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `child` (`child`,`parent`,`type`,`world`),
  KEY `child_2` (`child`,`type`),
  KEY `parent` (`parent`,`type`)
) DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
