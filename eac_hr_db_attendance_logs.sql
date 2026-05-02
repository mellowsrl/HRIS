CREATE DATABASE  IF NOT EXISTS `eac_hr_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `eac_hr_db`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: eac_hr_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attendance_logs`
--

DROP TABLE IF EXISTS `attendance_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_logs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `employee_id` int NOT NULL,
  `log_date` date NOT NULL,
  `time_in` time DEFAULT NULL,
  `time_out` time DEFAULT NULL,
  `undertime_minutes` int DEFAULT '0',
  `leave_type` varchar(255) DEFAULT NULL,
  `work_code` varchar(255) DEFAULT NULL,
  `day_type` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  CONSTRAINT `fk_attendance_employee` FOREIGN KEY (`employee_id`) REFERENCES `official_employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_logs`
--

LOCK TABLES `attendance_logs` WRITE;
/*!40000 ALTER TABLE `attendance_logs` DISABLE KEYS */;
INSERT INTO `attendance_logs` VALUES (1,1,'2026-02-22',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(2,2,'2026-02-22',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(3,3,'2026-02-22',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(4,1,'2026-02-23','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(5,2,'2026-02-23','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(6,3,'2026-02-23','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(7,1,'2026-02-24','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(8,2,'2026-02-24','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(9,3,'2026-02-24','13:00:00','17:00:00',240,NULL,'Normal','Workday','PRESENT'),(10,1,'2026-02-25',NULL,NULL,0,NULL,'Normal','Holiday','ABSENT'),(11,2,'2026-02-25',NULL,NULL,0,NULL,'Normal','Holiday','ABSENT'),(12,3,'2026-02-25',NULL,NULL,0,NULL,'Normal','Holiday','ABSENT'),(13,1,'2026-02-26','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(14,2,'2026-02-26','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(15,3,'2026-02-26','13:00:00','17:00:00',240,NULL,'Normal','Workday','PRESENT'),(16,1,'2026-02-27','08:00:00','17:00:00',0,'VL','Normal','Workday','PAID_VL'),(17,2,'2026-02-27','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(18,3,'2026-02-27','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(19,1,'2026-02-28',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(20,2,'2026-02-28',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(21,3,'2026-02-28',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(22,1,'2026-03-01',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(23,2,'2026-03-01',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(24,3,'2026-03-01',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(25,1,'2026-03-02','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(26,2,'2026-03-02','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(27,3,'2026-03-02','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(28,1,'2026-03-03','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(29,2,'2026-03-03','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(30,3,'2026-03-03',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(31,1,'2026-03-04','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(32,2,'2026-03-04','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(33,3,'2026-03-04','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(34,1,'2026-03-05','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(35,2,'2026-03-05',NULL,NULL,0,'STL','Normal','Workday','STL'),(36,3,'2026-03-05','13:00:00','17:00:00',240,NULL,'Normal','Workday','PRESENT'),(37,1,'2026-03-06','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(38,2,'2026-03-06','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(39,3,'2026-03-06','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(40,1,'2026-03-07',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(41,2,'2026-03-07',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(42,3,'2026-03-07',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(43,1,'2026-03-08',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(44,2,'2026-03-08',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(45,3,'2026-03-08',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(46,1,'2026-03-09','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(47,2,'2026-03-09','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(48,3,'2026-03-09','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(49,1,'2026-03-10','08:00:00','17:00:00',0,'SL','Normal','Workday','PAID_SL'),(50,2,'2026-03-10','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(51,3,'2026-03-10',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(52,1,'2026-03-11','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(53,2,'2026-03-11','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(54,3,'2026-03-11','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(55,1,'2026-03-12','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(56,2,'2026-03-12','08:00:00','18:00:00',60,NULL,'Normal','Workday','PRESENT'),(57,3,'2026-03-12','13:00:00','17:00:00',240,NULL,'Normal','Workday','PRESENT'),(58,1,'2026-03-13','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(59,2,'2026-03-13','07:00:00','18:00:00',0,NULL,'Normal','Workday','PRESENT'),(60,3,'2026-03-13','08:00:00','12:00:00',240,NULL,'Normal','Workday','PRESENT'),(61,1,'2026-03-14',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(62,2,'2026-03-14',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(63,3,'2026-03-14',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(64,1,'2026-03-15',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(65,2,'2026-03-15',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(66,3,'2026-03-15',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(67,1,'2026-03-16','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(68,2,'2026-03-16','03:23:00','21:52:00',0,NULL,'Normal','Workday','PRESENT'),(69,3,'2026-03-16','07:57:00','21:47:00',0,NULL,'Normal','Workday','PRESENT'),(70,1,'2026-03-17','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(71,2,'2026-03-17',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(72,3,'2026-03-17',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(73,1,'2026-03-18','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(74,2,'2026-03-18',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(75,3,'2026-03-18',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(76,1,'2026-03-19','08:00:00','17:00:00',0,NULL,'Normal','Workday','PRESENT'),(77,2,'2026-03-19',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(78,3,'2026-03-19',NULL,NULL,0,'Absent','Normal','Workday','Absent'),(79,1,'2026-03-20',NULL,NULL,0,NULL,'Normal','Holiday','ABSENT'),(80,2,'2026-03-20',NULL,NULL,0,NULL,'Normal','Holiday','ABSENT'),(81,3,'2026-03-20',NULL,NULL,0,NULL,'Normal','Holiday','ABSENT'),(82,1,'2026-03-21',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(83,2,'2026-03-21',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(84,3,'2026-03-21',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(85,1,'2026-03-22',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(86,2,'2026-03-22',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(87,3,'2026-03-22',NULL,NULL,0,NULL,'Normal','Restday','REST DAY'),(88,2,'2026-04-21','08:00:00','17:00:00',0,'Vacation Leave','Normal','Workday','Vacation Leave'),(89,2,'2026-04-22','08:00:00','17:00:00',0,'Vacation Leave','Normal','Workday','Vacation Leave'),(90,2,'2026-04-23','08:00:00','17:00:00',0,'Vacation Leave','Normal','Workday','Vacation Leave'),(91,2,'2026-04-24','08:00:00','17:00:00',0,'Vacation Leave','Normal','Workday','Vacation Leave'),(92,2,'2026-04-20','08:00:00','17:00:00',0,'Vacation Leave','Normal','Workday','Vacation Leave');
/*!40000 ALTER TABLE `attendance_logs` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-20 22:59:28
