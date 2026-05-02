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
-- Table structure for table `official_employees`
--

DROP TABLE IF EXISTS `official_employees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `official_employees` (
  `id` int NOT NULL AUTO_INCREMENT,
  `custom_employee_id` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `department` varchar(255) DEFAULT NULL,
  `position` varchar(255) DEFAULT NULL,
  `employment_type` varchar(255) DEFAULT NULL,
  `payment_type` varchar(255) DEFAULT NULL,
  `expected_shift` varchar(255) DEFAULT NULL,
  `daily_wage` double DEFAULT '0',
  `vl_balance` int DEFAULT '15',
  `sl_balance` int DEFAULT '15',
  `ml_balance` int DEFAULT '0',
  `pl_balance` int DEFAULT '0',
  `spl_balance` int DEFAULT '0',
  `bl_balance` int DEFAULT '0',
  `incentive_leave_balance` int DEFAULT '5',
  `study_leave_balance` int DEFAULT '0',
  `emergency_contact_name` varchar(255) DEFAULT NULL,
  `emergency_contact_phone` varchar(255) DEFAULT NULL,
  `highest_degree` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `years_experience` int DEFAULT '0',
  `previous_employer` varchar(255) DEFAULT NULL,
  `resume_link` varchar(255) DEFAULT NULL,
  `sss_number` varchar(255) DEFAULT NULL,
  `tin_number` varchar(255) DEFAULT NULL,
  `philhealth_number` varchar(255) DEFAULT NULL,
  `pagibig_number` varchar(255) DEFAULT NULL,
  `biometric_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `custom_employee_id` (`custom_employee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `official_employees`
--

LOCK TABLES `official_employees` WRITE;
/*!40000 ALTER TABLE `official_employees` DISABLE KEYS */;
INSERT INTO `official_employees` VALUES (1,'11001','Franklin','Moris','bushmoris1@gmail.com','09602848942','Male','IT Operations','Staff','Full-Time Non-Faculty','Semi-Monthly','08:00 AM - 05:00 PM',1000,15,15,0,7,7,3,5,0,'N/A','N/A','N/A','HIRED',1,'José Rizal University','https://drive.google.com/drive/folders/16VAnCby466TUs9rujtUiG3Zas_-uqMwG',NULL,NULL,NULL,NULL,2),(2,'12001','Atasha','Lumagbas','gwenatasha.lumagbas.mnl@eac.edu.ph','09602848942','Female','School of Nursing','SET Department Head','Full-Time','Semi-Monthly','08:00 AM - 05:00 PM',1000,10,15,105,0,7,3,5,0,'N/A','N/A','N/A','HIRED',1,'José Rizal University','https://drive.google.com/drive/folders/16VAnCby466TUs9rujtUiG3Zas_-uqMwG',NULL,NULL,NULL,NULL,3),(3,'21001','Maron','Javier','maronthegreatt@gmail.com','09602848942','Male','IT Operations','Maintenance Staff','Part-Time Non-Faculty','Semi-Monthly','08:00 AM - 05:00 PM',1000,15,15,0,7,7,3,5,0,'N/A','N/A','N/A','HIRED',1,'José Rizal University','https://drive.google.com/drive/folders/16VAnCby466TUs9rujtUiG3Zas_-uqMwG',NULL,NULL,NULL,NULL,4),(4,'11002','Reinz','Mamuyac','bushmoris1239@gmail.com','09602848942','Male','IT Operations','IT Support','Full-Time Non-Faculty','Semi-Monthly','08:00 AM - 05:00 PM',1000,15,15,0,7,7,3,5,0,'N/A','N/A','N/A','HIRED',1,'José Rizal University','https://drive.google.com/drive/folders/16VAnCby466TUs9rujtUiG3Zas_-uqMwG',NULL,NULL,NULL,NULL,5),(7,'12002','Ralph','Lunas','lunasgabriel7@gmail.com','09602848942','Male','School of Nursing','Professor','Full-Time','Semi-Monthly','08:00 AM - 05:00 PM',1000,15,15,0,7,7,3,5,0,'N/A','N/A','N/A','HIRED',1,'José Rizal University','https://drive.google.com/drive/folders/16VAnCby466TUs9rujtUiG3Zas_-uqMwG',NULL,NULL,NULL,NULL,6);
/*!40000 ALTER TABLE `official_employees` ENABLE KEYS */;
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
