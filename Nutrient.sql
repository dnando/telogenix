/*
 Navicat MySQL Data Transfer

 Source Server         : Local
 Source Server Type    : MySQL
 Source Server Version : 50525
 Source Host           : localhost:3306
 Source Schema         : telogenix

 Target Server Type    : MySQL
 Target Server Version : 50525
 File Encoding         : 65001

 Date: 21/08/2020 02:15:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for Nutrient
-- ----------------------------
DROP TABLE IF EXISTS `Nutrient`;
CREATE TABLE `Nutrient` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `grams_in_stock` int(11) DEFAULT NULL,
  `note` text COLLATE utf8_bin,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

SET FOREIGN_KEY_CHECKS = 1;
