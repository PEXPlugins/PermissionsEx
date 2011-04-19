CREATE TABLE IF NOT EXISTS `permissions` (
  `name` varchar(255) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `permission` varchar(255) NOT NULL,
  `world` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  UNIQUE KEY `unique` (`name`,`permission`,`world`,`type`),
  KEY `user` (`name`,`type`),
  KEY `world` (`world`,`name`,`type`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `permissions_entity` (
  `name` varchar(255) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `prefix` varchar(255) NOT NULL,
  `suffix` varchar(255) NOT NULL,
  `default` tinyint(1) NOT NULL,
  PRIMARY KEY (`name`),
  KEY `default` (`default`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO `permissions_entity` (`name`, `type`, `prefix`, `suffix`, `default`) VALUES
('default', 0, '', '', 1);

CREATE TABLE IF NOT EXISTS `permissions_inheritance` (
  `child` varchar(255) NOT NULL,
  `parent` varchar(255) NOT NULL,
  `type` tinyint(1) NOT NULL COMMENT '0 for group-group, 1 for user-group',
  UNIQUE KEY `child` (`child`,`parent`,`type`),
  KEY `child_2` (`child`,`type`),
  KEY `parent` (`parent`,`type`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
