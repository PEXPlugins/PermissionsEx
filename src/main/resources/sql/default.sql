CREATE TABLE IF NOT EXISTS `groups` (
  `group` varchar(255) NOT NULL,
  `parents` text NOT NULL,
  `prefix` varchar(255) NOT NULL,
  `suffix` varchar(255) NOT NULL,
  `default` tinyint(1) NOT NULL,
  PRIMARY KEY (`group`),
  KEY `default` (`default`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO `groups` (`group`, `parents`, `prefix`, `suffix`, `default`) VALUES
('default', '', '', '', 1);

CREATE TABLE IF NOT EXISTS `group_permissions` (
  `group` varchar(255) NOT NULL,
  `permission` varchar(255) NOT NULL,
  `world` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  UNIQUE KEY `unique` (`group`,`permission`,`world`),
  KEY `world` (`group`,`world`),
  KEY `group` (`group`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO `group_permissions` (`group`, `permission`, `world`, `value`) VALUES
('default', 'modifyworld.*', '', 'true');

CREATE TABLE IF NOT EXISTS `users` (
  `username` varchar(255) NOT NULL,
  `prefix` varchar(255) NOT NULL,
  `suffix` varchar(255) NOT NULL,
  `group` varchar(255) NOT NULL DEFAULT 'default',
  PRIMARY KEY (`username`),
  KEY `group` (`group`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;


CREATE TABLE IF NOT EXISTS `user_permissions` (
  `user` varchar(255) NOT NULL,
  `permission` varchar(255) NOT NULL,
  `world` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  UNIQUE KEY `unique` (`user`,`permission`,`world`),
  KEY `world` (`world`,`user`),
  KEY `user` (`user`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
