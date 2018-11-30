CREATE TABLE `change_frequency` (
  `id` int(11) NOT NULL,
  `change_frequency` int(11) NOT NULL DEFAULT '0',
  `last_loc_count` int(11) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
