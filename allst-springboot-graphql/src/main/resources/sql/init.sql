CREATE TABLE IF NOT EXISTS `book`
(
    `id`        int(20) NOT NULL AUTO_INCREMENT,
    `name`      varchar(255) DEFAULT NULL,
    `pageCount` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_name` (`name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8;
select * from `book`;
INSERT INTO `book` (`id`, `name`, `pageCount`) VALUES (1, 'the golden ticket', '255');
INSERT INTO `book` (`id`, `name`, `pageCount`) VALUES (2, 'coding game', '400');
INSERT INTO `book` (`id`, `name`, `pageCount`) VALUES (3, 'java', '500');
INSERT INTO `book` (`id`, `name`, `pageCount`) VALUES (4, 'c++', '650');
INSERT INTO `book` (`id`, `name`, `pageCount`) VALUES (5, 'scala', '700');
INSERT INTO `book` (`id`, `name`, `pageCount`) VALUES (6, 'python', '800');

CREATE TABLE IF NOT EXISTS `Author`
(
    `id`        INT(20)      NOT NULL AUTO_INCREMENT,
    `firstName` VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
    `lastName`  VARCHAR(255) NULL DEFAULT NULL COLLATE 'utf8_general_ci',
    `bookId`    INT(20)      NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `Index_name` (`firstName`) USING BTREE,
    INDEX `FK_Author_Book` (`bookId`) USING BTREE,
    CONSTRAINT `FK_Author_Book` FOREIGN KEY (`bookId`) REFERENCES `demo`.`Book` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
) COLLATE = 'utf8_general_ci'
  ENGINE = InnoDB
  AUTO_INCREMENT = 1;
select * from `Author` ;
INSERT INTO `Author` (`id`, `firstName`, `lastName`, `bookId`)
values (1, 'J. R. R.', 'Tolkien', 1),
(7, 'Joanne', 'Rowling', 1),
(2, 'J. K.', 'Rowling', 1),
(3, 'Stephen', 'King', 2),
(4, 'Charles', 'Dickens', 3),
(5, 'Jane', 'Austen', 4),
(6, 'Leo', 'Tolstoy', 5);
