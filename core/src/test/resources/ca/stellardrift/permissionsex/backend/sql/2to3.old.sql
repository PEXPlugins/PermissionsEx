


DROP TABLE IF EXISTS `permissions`;
CREATE TABLE `permissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `permission` mediumtext NOT NULL,
  `world` varchar(50) NOT NULL,
  `value` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user` (`name`,`type`),
  KEY `world` (`world`,`name`,`type`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4;


INSERT INTO `permissions` VALUES (1,'system',2,'schema_version','','2'),(2,'default',0,'modifyworld.*','',''),(3,'default',0,'default','','true'),(5,'member',0,'prefix','','[Member]'),(6,'member',0,'worldedit.navigation.jumpto','',''),(7,'member',0,'commandbook.tp.*','world_nether',''),(8,'admin',0,'*','',''),(9,'2f224fdf-ca9a-4043-8166-0d673ba4c0b8',1,'name','','zml'),(10,'2f224fdf-ca9a-4043-8166-0d673ba4c0b8',1,'another.perm','',''),(11,'default',0,'rank','','1000'),(12,'member',0,'rank','','900'),(14,'vip',0,'rank','','700'),(15,'loyalist',0,'rank','','800');


DROP TABLE IF EXISTS `permissions_entity`;
CREATE TABLE `permissions_entity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `default` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`,`type`),
  KEY `default` (`default`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4;


INSERT INTO `permissions_entity` VALUES (1,'default',0,0),(2,'member',0,0),(3,'admin',0,0),(4,'2f224fdf-ca9a-4043-8166-0d673ba4c0b8',1,0),(5,'loyalist',0,0),(6,'vip',0,0);


DROP TABLE IF EXISTS `permissions_inheritance`;
CREATE TABLE `permissions_inheritance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `child` varchar(50) NOT NULL,
  `parent` varchar(50) NOT NULL,
  `type` tinyint(1) NOT NULL,
  `world` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `child` (`child`,`parent`,`type`,`world`),
  KEY `child_2` (`child`,`type`),
  KEY `parent` (`parent`,`type`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;


INSERT INTO `permissions_inheritance` VALUES (1,'2f224fdf-ca9a-4043-8166-0d673ba4c0b8','admin',1,NULL),(3,'loyalist','member',0,NULL),(2,'member','default',0,NULL),(4,'vip','loyalist',0,NULL);


