CREATE TABLE IF NOT EXISTS `{groups}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` TEXT NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `{qualifiers}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group` int(11) NOT NULL,
  `key` VARCHAR(250) NOT NULL,
  `value` VARCHAR(250) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`group`, `key`, `value`),
  KEY (`key`),
  KEY (`key`, `value`),
  FOREIGN KEY (`group`) REFERENCES `{groups}` (`id`) ON DELETE CASCADE
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `{entries}` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group` int(11) NOT NULL,
  `key` TEXT NOT NULL,
  `value` TEXT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`group`) REFERENCES `{groups}` (`id`) ON DELETE CASCADE
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
