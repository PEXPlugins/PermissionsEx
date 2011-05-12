SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

CREATE TABLE IF NOT EXISTS `permissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `permission` varchar(255) NOT NULL,
  `world` varchar(255) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique` (`name`,`permission`,`world`,`type`),
  KEY `user` (`name`,`type`),
  KEY `world` (`world`,`name`,`type`)
);

INSERT INTO `permissions` (`id`, `name`, `type`, `permission`, `world`, `value`) VALUES
(1, 'default', 0, 'modifyworld.*', '', '');

CREATE TABLE IF NOT EXISTS `permissions_entity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `prefix` varchar(255) NOT NULL,
  `suffix` varchar(255) NOT NULL,
  `default` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `default` (`default`)
);

INSERT INTO `permissions_entity` (`id`, `name`, `type`, `prefix`, `suffix`, `default`) VALUES
(1, 'default', 0, '', '', 1);

CREATE TABLE IF NOT EXISTS `permissions_inheritance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `child` varchar(255) NOT NULL,
  `parent` varchar(255) NOT NULL,
  `type` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `child` (`child`,`parent`,`type`),
  KEY `child_2` (`child`,`type`),
  KEY `parent` (`parent`,`type`)
);

