CREATE TABLE `complexity` (
  `id` int(11) NOT NULL,
  `complexity` varchar(45) NOT NULL,
  `timestamp` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY `UNIQUE_COMPLEXITY` (`id`,`timestamp`),
  KEY `PRIMARY_KEY` (`id`,`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8