CREATE TABLE `change_sqale` (
  `id` int(11) NOT NULL,
  `old_sqale_rating` int(11) NOT NULL DEFAULT '0',
  `new_sqale_rating` int(11) NOT NULL DEFAULT '0',
  `old_sqale_ratio` double NOT NULL,
  `new_sqale_ratio` double NOT NULL,
  `change_sqale_ratio` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;