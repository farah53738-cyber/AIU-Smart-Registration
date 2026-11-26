-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: aiu_registration
-- ------------------------------------------------------
-- Server version	8.0.46

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
-- Table structure for table `auditlog`
--

DROP TABLE IF EXISTS `auditlog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auditlog` (
  `audit_id` bigint NOT NULL AUTO_INCREMENT,
  `actor_id` bigint NOT NULL,
  `action` varchar(60) NOT NULL,
  `entity_type` varchar(40) NOT NULL,
  `entity_id` bigint NOT NULL,
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `request_id` varchar(64) NOT NULL,
  PRIMARY KEY (`audit_id`),
  KEY `actor_id` (`actor_id`),
  CONSTRAINT `auditlog_ibfk_1` FOREIGN KEY (`actor_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auditlog`
--

LOCK TABLES `auditlog` WRITE;
/*!40000 ALTER TABLE `auditlog` DISABLE KEYS */;
INSERT INTO `auditlog` VALUES (1,1,'WITHDRAW','REQUEST',3,'2026-11-11 04:34:23','ef20acc1-f496-4671-ab30-4161d6ff14ee'),(2,3,'APPROVED','REQUEST',5,'2026-11-12 16:09:49','a83a1bb2-be67-498f-b491-e5afaa124bc5'),(3,1,'JOIN_WAITLIST','WAITLIST',1,'2026-11-12 17:28:38','316129b2-5574-4b80-87e2-8d761d6b33a0'),(4,1,'WITHDRAW','REQUEST',4,'2026-11-12 19:27:29','e494c34c-31aa-4ada-b6ae-d8c4b3f27140'),(5,2,'WITHDRAW','REQUEST',2,'2026-11-12 21:17:50','f81369fd-188a-4fa0-b5f0-83ee8c0880a7'),(6,2,'SUBMIT','REQUEST',6,'2026-11-12 21:18:15','0a912d8e-a1c2-4a5b-b901-c5c48e43a5a6'),(7,3,'APPROVED','REQUEST',7,'2026-11-12 21:57:46','49f52f0a-5204-409d-aca1-66b5a9b87e6f'),(8,3,'ADVISOR_REGISTER','REQUEST',7,'2026-11-12 21:57:46','c8f58cbb-a65d-4410-befb-d7fbad22f284'),(9,12,'PUBLISH_NOTICE','NOTICE',0,'2026-11-12 22:01:45','ff36c9a5-9a0d-4c5f-a327-2d71c0aa039f'),(10,3,'APPROVED','REQUEST',6,'2026-11-12 22:15:43','063432d6-8531-4e4c-82ec-1cd7143ffbd4'),(11,3,'APPROVED','REQUEST',8,'2026-11-12 22:16:21','52f856cf-60a2-4541-98df-fe169858739b'),(12,3,'ADVISOR_REGISTER','REQUEST',8,'2026-11-12 22:16:21','5c117280-e245-4e32-8b65-34588e2170ea'),(13,12,'SAVE_PREREQUISITE','COURSE',4,'2026-11-12 22:20:07','b9ad3972-bbaa-41ed-a3d6-65453025d69e');
/*!40000 ALTER TABLE `auditlog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `completedcourse`
--

DROP TABLE IF EXISTS `completedcourse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `completedcourse` (
  `completion_id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `term_id` bigint NOT NULL,
  `passed` tinyint(1) NOT NULL,
  `grade_label` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`completion_id`),
  UNIQUE KEY `student_id` (`student_id`,`course_id`,`term_id`),
  KEY `course_id` (`course_id`),
  KEY `term_id` (`term_id`),
  KEY `idx_completed_lookup` (`student_id`,`passed`,`course_id`),
  CONSTRAINT `completedcourse_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `studentprofile` (`user_id`),
  CONSTRAINT `completedcourse_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `completedcourse_ibfk_3` FOREIGN KEY (`term_id`) REFERENCES `term` (`term_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `completedcourse`
--

LOCK TABLES `completedcourse` WRITE;
/*!40000 ALTER TABLE `completedcourse` DISABLE KEYS */;
INSERT INTO `completedcourse` VALUES (1,1,3,1,1,'PASS');
/*!40000 ALTER TABLE `completedcourse` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course` (
  `course_id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL,
  `title` varchar(150) NOT NULL,
  `description` text,
  `credits` int NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course`
--

LOCK TABLES `course` WRITE;
/*!40000 ALTER TABLE `course` DISABLE KEYS */;
INSERT INTO `course` VALUES (1,'CSC101','Introduction to Programming','Programming fundamentals',3,1),(2,'CSC110','Object Oriented Programming','Classes and objects',3,1),(3,'CSC230','Programming with Python','Python programming',3,1),(4,'CSC310','Computer Organization','Computer hardware and architecture',3,1),(5,'CSC340','Software Engineering','Software development and design',3,1),(6,'CSC342','Operating Systems','Operating system concepts',3,1),(7,'CSC470','Database Systems','Database design and SQL',3,1),(8,'MAT122','Calculus II','Integration and mathematical applications',3,1);
/*!40000 ALTER TABLE `course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courseprerequisite`
--

DROP TABLE IF EXISTS `courseprerequisite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courseprerequisite` (
  `course_id` bigint NOT NULL,
  `prerequisite_id` bigint NOT NULL,
  PRIMARY KEY (`course_id`,`prerequisite_id`),
  KEY `prerequisite_id` (`prerequisite_id`),
  CONSTRAINT `courseprerequisite_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `courseprerequisite_ibfk_2` FOREIGN KEY (`prerequisite_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `courseprerequisite_chk_1` CHECK ((`course_id` <> `prerequisite_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courseprerequisite`
--

LOCK TABLES `courseprerequisite` WRITE;
/*!40000 ALTER TABLE `courseprerequisite` DISABLE KEYS */;
INSERT INTO `courseprerequisite` VALUES (4,2),(5,3);
/*!40000 ALTER TABLE `courseprerequisite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollment`
--

DROP TABLE IF EXISTS `enrollment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollment` (
  `enrollment_id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  `request_id` bigint DEFAULT NULL,
  `status` enum('ACTIVE','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `student_id` (`student_id`,`section_id`),
  KEY `request_id` (`request_id`),
  KEY `idx_enrollment_seats` (`section_id`,`status`),
  CONSTRAINT `enrollment_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `studentprofile` (`user_id`),
  CONSTRAINT `enrollment_ibfk_2` FOREIGN KEY (`section_id`) REFERENCES `section` (`section_id`),
  CONSTRAINT `enrollment_ibfk_3` FOREIGN KEY (`request_id`) REFERENCES `registrationrequest` (`request_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollment`
--

LOCK TABLES `enrollment` WRITE;
/*!40000 ALTER TABLE `enrollment` DISABLE KEYS */;
INSERT INTO `enrollment` VALUES (1,1,8,5,'ACTIVE','2026-11-12 16:09:49'),(2,1,1,7,'ACTIVE','2026-11-12 21:57:46'),(3,2,2,6,'ACTIVE','2026-11-12 22:15:43'),(4,2,7,6,'ACTIVE','2026-11-12 22:15:43'),(5,2,4,8,'ACTIVE','2026-11-12 22:16:21');
/*!40000 ALTER TABLE `enrollment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `type` varchar(40) NOT NULL,
  `message` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` datetime DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  UNIQUE KEY `event_id` (`event_id`,`user_id`),
  KEY `idx_notification_inbox` (`user_id`,`read_at`),
  CONSTRAINT `notification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `notification_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `outboxevent` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
INSERT INTO `notification` VALUES (1,1,1,'DECISION','Request #5 approved','2026-11-12 16:09:49','2026-11-12 17:22:28'),(2,1,2,'WAITLIST','You joined the waitlist for CSC110. A notification does not reserve a seat.','2026-11-12 17:28:38','2026-11-12 17:32:35'),(3,2,3,'SUBMITTED','Your registration request has been sent to your academic advisor for review.','2026-11-12 21:18:15',NULL),(4,3,4,'ADVISOR_REQUEST','A student has submitted a registration request for your review.','2026-11-12 21:18:15','2026-11-12 21:33:37'),(5,1,5,'DECISION','Your registration request has been approved. Reason: Registered by academic advisor.','2026-11-12 21:57:46','2026-11-12 22:12:31'),(6,1,6,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45','2026-11-12 22:12:31'),(7,2,7,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(8,3,8,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45','2026-11-12 22:15:23'),(9,4,9,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(10,5,10,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(11,6,11,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(12,7,12,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(13,8,13,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(14,9,14,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(15,10,15,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(16,11,16,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(17,12,17,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45','2026-11-12 22:01:55'),(18,13,18,'REGISTRATION_NOTICE','hi all','2026-11-12 22:01:45',NULL),(19,2,19,'DECISION','Your registration request has been approved.','2026-11-12 22:15:43',NULL),(20,2,20,'DECISION','Your registration request has been approved. Reason: Registered by academic advisor.','2026-11-12 22:16:21',NULL);
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `outboxevent`
--

DROP TABLE IF EXISTS `outboxevent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outboxevent` (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(40) NOT NULL,
  `aggregate_id` bigint NOT NULL,
  `payload` json NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` datetime DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`event_id`),
  KEY `idx_outbox_pending` (`processed_at`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `outboxevent`
--

LOCK TABLES `outboxevent` WRITE;
/*!40000 ALTER TABLE `outboxevent` DISABLE KEYS */;
INSERT INTO `outboxevent` VALUES (1,'DECISION',5,'{}','2026-11-12 16:09:49','2026-11-12 16:09:49',0),(2,'WAITLIST',1,'{}','2026-11-12 17:28:38','2026-11-12 17:28:38',0),(3,'SUBMITTED',6,'{}','2026-11-12 21:18:15','2026-11-12 21:18:15',0),(4,'ADVISOR_REQUEST',6,'{}','2026-11-12 21:18:15','2026-11-12 21:18:15',0),(5,'DECISION',7,'{}','2026-11-12 21:57:46','2026-11-12 21:57:46',0),(6,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(7,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(8,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(9,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(10,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(11,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(12,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(13,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(14,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(15,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(16,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(17,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(18,'REGISTRATION_NOTICE',0,'{}','2026-11-12 22:01:45','2026-11-12 22:01:45',0),(19,'DECISION',6,'{}','2026-11-12 22:15:43','2026-11-12 22:15:43',0),(20,'DECISION',8,'{}','2026-11-12 22:16:21','2026-11-12 22:16:21',0);
/*!40000 ALTER TABLE `outboxevent` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plan`
--

DROP TABLE IF EXISTS `plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `term_id` bigint NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `student_id` (`student_id`,`term_id`),
  KEY `term_id` (`term_id`),
  CONSTRAINT `plan_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `studentprofile` (`user_id`),
  CONSTRAINT `plan_ibfk_2` FOREIGN KEY (`term_id`) REFERENCES `term` (`term_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plan`
--

LOCK TABLES `plan` WRITE;
/*!40000 ALTER TABLE `plan` DISABLE KEYS */;
INSERT INTO `plan` VALUES (1,1,1,29,'2026-11-12 22:14:08'),(2,2,1,2,'2026-11-12 21:18:00');
/*!40000 ALTER TABLE `plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `planitem`
--

DROP TABLE IF EXISTS `planitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `planitem` (
  `plan_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  PRIMARY KEY (`plan_id`,`section_id`),
  KEY `section_id` (`section_id`),
  CONSTRAINT `planitem_ibfk_1` FOREIGN KEY (`plan_id`) REFERENCES `plan` (`plan_id`),
  CONSTRAINT `planitem_ibfk_2` FOREIGN KEY (`section_id`) REFERENCES `section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `planitem`
--

LOCK TABLES `planitem` WRITE;
/*!40000 ALTER TABLE `planitem` DISABLE KEYS */;
INSERT INTO `planitem` VALUES (2,2),(1,7),(2,7);
/*!40000 ALTER TABLE `planitem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registrationrequest`
--

DROP TABLE IF EXISTS `registrationrequest`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registrationrequest` (
  `request_id` bigint NOT NULL AUTO_INCREMENT,
  `student_name` varchar(100) NOT NULL,
  `student_id` varchar(20) NOT NULL,
  `section_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `requested_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `student_user_id` bigint DEFAULT NULL,
  `term_id` bigint DEFAULT NULL,
  `decided_at` datetime DEFAULT NULL,
  `decided_by` bigint DEFAULT NULL,
  `reason` text,
  `version` int NOT NULL DEFAULT '0',
  `idempotency_key` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  UNIQUE KEY `uq_student_idempotency` (`student_user_id`,`idempotency_key`),
  KEY `section_id` (`section_id`),
  KEY `fk_request_term` (`term_id`),
  KEY `fk_request_decider` (`decided_by`),
  KEY `idx_request_status_term` (`student_user_id`,`term_id`,`status`),
  CONSTRAINT `fk_request_decider` FOREIGN KEY (`decided_by`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_request_term` FOREIGN KEY (`term_id`) REFERENCES `term` (`term_id`),
  CONSTRAINT `fk_request_user` FOREIGN KEY (`student_user_id`) REFERENCES `studentprofile` (`user_id`),
  CONSTRAINT `registrationrequest_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `section` (`section_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registrationrequest`
--

LOCK TABLES `registrationrequest` WRITE;
/*!40000 ALTER TABLE `registrationrequest` DISABLE KEYS */;
INSERT INTO `registrationrequest` VALUES (1,'Sarah','1',3,'LEGACY_INVALID','2026-10-27 23:33:09',1,1,NULL,NULL,'Legacy prototype approval without an enrollment record; historical data corrected.',0,NULL),(2,'Demo Student','2',3,'WITHDRAWN','2026-10-29 20:45:42',2,1,NULL,NULL,NULL,1,NULL),(3,'Sarah','1',5,'WITHDRAWN','2026-11-04 03:39:14',1,1,NULL,NULL,NULL,1,NULL),(4,'Sarah','1',7,'WITHDRAWN','2026-11-04 03:47:58',1,1,NULL,NULL,NULL,1,NULL),(5,'Sarah','1',8,'APPROVED','2026-11-04 03:48:31',1,1,'2026-11-12 16:09:49',3,NULL,1,NULL),(6,'Demo Student','student2',2,'APPROVED','2026-11-12 18:18:15',2,1,'2026-11-12 22:15:43',3,NULL,1,'93d9bded-fdea-46cc-8bdf-bd85a32ae22d'),(7,'Sarah','student1',1,'APPROVED','2026-11-12 18:57:46',1,1,'2026-11-12 21:57:46',3,'Registered by academic advisor.',1,'advisor:3d56c8d4-cc4c-4774-b927-f35a5aa1f4dd'),(8,'Demo Student','student2',4,'APPROVED','2026-11-12 19:16:21',2,1,'2026-11-12 22:16:21',3,'Registered by academic advisor.',1,'advisor:cb66671a-9d95-4657-ab27-50f9a3a2c8e1');
/*!40000 ALTER TABLE `registrationrequest` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `requestitem`
--

DROP TABLE IF EXISTS `requestitem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `requestitem` (
  `request_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  PRIMARY KEY (`request_id`,`section_id`),
  KEY `section_id` (`section_id`),
  CONSTRAINT `requestitem_ibfk_1` FOREIGN KEY (`request_id`) REFERENCES `registrationrequest` (`request_id`),
  CONSTRAINT `requestitem_ibfk_2` FOREIGN KEY (`section_id`) REFERENCES `section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `requestitem`
--

LOCK TABLES `requestitem` WRITE;
/*!40000 ALTER TABLE `requestitem` DISABLE KEYS */;
INSERT INTO `requestitem` VALUES (7,1),(6,2),(1,3),(2,3),(8,4),(3,5),(4,7),(6,7),(5,8);
/*!40000 ALTER TABLE `requestitem` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `section`
--

DROP TABLE IF EXISTS `section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `section` (
  `section_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `section_number` varchar(10) NOT NULL,
  `instructor` varchar(100) NOT NULL,
  `schedule` varchar(100) NOT NULL,
  `room` varchar(30) NOT NULL,
  `capacity` int NOT NULL,
  `term_id` bigint DEFAULT NULL,
  `instructor_id` bigint DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`section_id`),
  KEY `course_id` (`course_id`),
  KEY `idx_section_term_course` (`term_id`,`course_id`),
  KEY `idx_section_instructor` (`instructor_id`),
  CONSTRAINT `fk_section_instructor` FOREIGN KEY (`instructor_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_section_term` FOREIGN KEY (`term_id`) REFERENCES `term` (`term_id`),
  CONSTRAINT `section_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`),
  CONSTRAINT `chk_section_capacity` CHECK ((`capacity` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `section`
--

LOCK TABLES `section` WRITE;
/*!40000 ALTER TABLE `section` DISABLE KEYS */;
INSERT INTO `section` VALUES (1,1,'01','Dr. Ahmed','Sun/Tue 9:00–10:15','A101',30,1,4,1),(2,2,'01','Dr. Sara','Mon/Wed 10:00–11:15','A102',30,1,5,1),(3,3,'01','Dr. Omar','Sun/Tue 11:00–12:15','B201',25,1,6,1),(4,4,'01','Dr. Noor','Mon/Wed 12:00–1:15','B202',30,1,7,1),(5,5,'01','Dr. Ali','Sun/Tue 1:00–2:15','C101',30,1,8,1),(6,6,'01','Dr. Fatima','Mon/Wed 9:00–10:15','C102',25,1,9,1),(7,7,'01','Dr. Layla','Sun/Tue 10:00–11:15','D101',30,1,10,1),(8,8,'01','Dr. Khalid','Mon/Wed 1:00–2:15','D102',35,1,11,1);
/*!40000 ALTER TABLE `section` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sectionmeeting`
--

DROP TABLE IF EXISTS `sectionmeeting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sectionmeeting` (
  `meeting_id` bigint NOT NULL AUTO_INCREMENT,
  `section_id` bigint NOT NULL,
  `day_of_week` tinyint NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `room` varchar(40) NOT NULL,
  PRIMARY KEY (`meeting_id`),
  KEY `idx_section_meeting` (`section_id`,`day_of_week`),
  CONSTRAINT `sectionmeeting_ibfk_1` FOREIGN KEY (`section_id`) REFERENCES `section` (`section_id`),
  CONSTRAINT `sectionmeeting_chk_1` CHECK ((`day_of_week` between 1 and 7)),
  CONSTRAINT `sectionmeeting_chk_2` CHECK ((`end_time` > `start_time`)),
  CONSTRAINT `sectionmeeting_chk_3` CHECK ((`end_date` >= `start_date`))
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sectionmeeting`
--

LOCK TABLES `sectionmeeting` WRITE;
/*!40000 ALTER TABLE `sectionmeeting` DISABLE KEYS */;
INSERT INTO `sectionmeeting` VALUES (1,1,7,'09:00:00','10:15:00','2026-09-01','2026-12-31','A101'),(2,1,2,'09:00:00','10:15:00','2026-09-01','2026-12-31','A101'),(3,2,1,'10:00:00','11:15:00','2026-09-01','2026-12-31','A102'),(4,2,3,'10:00:00','11:15:00','2026-09-01','2026-12-31','A102'),(5,3,7,'11:00:00','12:15:00','2026-09-01','2026-12-31','B201'),(6,3,2,'11:00:00','12:15:00','2026-09-01','2026-12-31','B201'),(7,4,1,'12:00:00','13:15:00','2026-09-01','2026-12-31','B202'),(8,4,3,'12:00:00','13:15:00','2026-09-01','2026-12-31','B202'),(9,5,7,'13:00:00','14:15:00','2026-09-01','2026-12-31','C101'),(10,5,2,'13:00:00','14:15:00','2026-09-01','2026-12-31','C101'),(11,6,1,'09:00:00','10:15:00','2026-09-01','2026-12-31','C102'),(12,6,3,'09:00:00','10:15:00','2026-09-01','2026-12-31','C102'),(13,7,7,'10:00:00','11:15:00','2026-09-01','2026-12-31','D101'),(14,7,2,'10:00:00','11:15:00','2026-09-01','2026-12-31','D101'),(15,8,1,'13:00:00','14:15:00','2026-09-01','2026-12-31','D102'),(16,8,3,'13:00:00','14:15:00','2026-09-01','2026-12-31','D102');
/*!40000 ALTER TABLE `sectionmeeting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `studentprofile`
--

DROP TABLE IF EXISTS `studentprofile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `studentprofile` (
  `user_id` bigint NOT NULL,
  `advisor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  KEY `advisor_id` (`advisor_id`),
  CONSTRAINT `studentprofile_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
  CONSTRAINT `studentprofile_ibfk_2` FOREIGN KEY (`advisor_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `studentprofile`
--

LOCK TABLES `studentprofile` WRITE;
/*!40000 ALTER TABLE `studentprofile` DISABLE KEYS */;
INSERT INTO `studentprofile` VALUES (1,3),(2,3);
/*!40000 ALTER TABLE `studentprofile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `term`
--

DROP TABLE IF EXISTS `term`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `term` (
  `term_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `registration_open` datetime NOT NULL,
  `registration_close` datetime NOT NULL,
  PRIMARY KEY (`term_id`),
  UNIQUE KEY `name` (`name`),
  CONSTRAINT `term_chk_1` CHECK ((`end_date` > `start_date`)),
  CONSTRAINT `term_chk_2` CHECK ((`registration_close` > `registration_open`))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `term`
--

LOCK TABLES `term` WRITE;
/*!40000 ALTER TABLE `term` DISABLE KEYS */;
INSERT INTO `term` VALUES (1,'Fall 2026 (Demo)','2026-09-01','2026-12-31','2026-09-01 00:00:00','2026-12-31 23:59:59');
/*!40000 ALTER TABLE `term` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `university_id` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `university_id` (`university_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'student1','Sarah','{bcrypt}$2a$10$Cx6fT0lNS8xHsjOgheADNOakL1wfgp4JULHgaw4z/6dRtTQZqaTMW',1),(2,'student2','Demo Student','{bcrypt}$2a$10$FD2HMTMzCdqcYvpdCluXhu0hGG637aAyP2s.L3N8/2jVjeOVCBdca',1),(3,'advisor','Demo Advisor','{bcrypt}$2a$10$zw0HsIGmhSawmZaFECxdBuwCMx6Ll.eU4emT/LI1Df4cJajMfUw9e',1),(4,'instructor_ahmed','Dr. Ahmed','{bcrypt}$2a$10$XnsrQp9Mw4VADUoH9JHnZuqPpZM.nRlbEril6JiPYCGtE.crNIgse',1),(5,'instructor_sara','Dr. Sara','{bcrypt}$2a$10$gu1dN7FZOzozjkIr0ZOyCeOpGXdOAK4fP2qbU0.jP7qyftd1NNBRG',1),(6,'instructor_omar','Dr. Omar','{bcrypt}$2a$10$QvFJ5UEuVKhfeR3mNAGvv.DMPSWBd4YjBhH7jZeGjeG00W7q4eHRq',1),(7,'instructor_noor','Dr. Noor','{bcrypt}$2a$10$mlx0uMUhqphgHGO4a8SBVu2JyPNfsq78pcaTy2dK22/IbD70/gtL.',1),(8,'instructor_ali','Dr. Ali','{bcrypt}$2a$10$Z20k72dKHXaCk2ps/DbosOVqajp7R89iD1wj6h0EjqfuIsqLRbbX.',1),(9,'instructor_fatima','Dr. Fatima','{bcrypt}$2a$10$CjRSUxsXyqj1VjYSlzL/I.E5dNGcwbB2e86vNkLOd7GmngpHonR06',1),(10,'instructor_layla','Dr. Layla','{bcrypt}$2a$10$qloa8oCue0SSjgeSYQ/PiOUMOXTEumWZHYD68FBjRHYHA1Wxuwv/u',1),(11,'instructor_khalid','Dr. Khalid','{bcrypt}$2a$10$wyeZ/lhc1fd.7mAUpte8Du9/GkGyg2O8PyJAS0D0TNHjcSn49gRT2',1),(12,'registrar','Demo Registrar','{bcrypt}$2a$10$cGOqNUmavTROFwAAbDTRheqY6ESc4Cvq9NspzBNlph9gvWDyawSKm',1),(13,'admin','Demo Administrator','{bcrypt}$2a$10$EugSwX12/UZYtHNZ48zlxeSocyelEhMehOeGeE.PXOARbUXRqGWTy',1);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `userrole`
--

DROP TABLE IF EXISTS `userrole`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userrole` (
  `user_id` bigint NOT NULL,
  `role` enum('STUDENT','ADVISOR','INSTRUCTOR','REGISTRAR','ADMIN') NOT NULL,
  PRIMARY KEY (`user_id`,`role`),
  CONSTRAINT `userrole_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userrole`
--

LOCK TABLES `userrole` WRITE;
/*!40000 ALTER TABLE `userrole` DISABLE KEYS */;
INSERT INTO `userrole` VALUES (1,'STUDENT'),(2,'STUDENT'),(3,'ADVISOR'),(4,'INSTRUCTOR'),(5,'INSTRUCTOR'),(6,'INSTRUCTOR'),(7,'INSTRUCTOR'),(8,'INSTRUCTOR'),(9,'INSTRUCTOR'),(10,'INSTRUCTOR'),(11,'INSTRUCTOR'),(12,'REGISTRAR'),(13,'ADMIN');
/*!40000 ALTER TABLE `userrole` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `waitlistentry`
--

DROP TABLE IF EXISTS `waitlistentry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `waitlistentry` (
  `waitlist_id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  `status` enum('ACTIVE','LEFT','FULFILLED') NOT NULL DEFAULT 'ACTIVE',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`waitlist_id`),
  UNIQUE KEY `student_id` (`student_id`,`section_id`),
  KEY `idx_waitlist_seats` (`section_id`,`status`,`joined_at`),
  CONSTRAINT `waitlistentry_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `studentprofile` (`user_id`),
  CONSTRAINT `waitlistentry_ibfk_2` FOREIGN KEY (`section_id`) REFERENCES `section` (`section_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `waitlistentry`
--

LOCK TABLES `waitlistentry` WRITE;
/*!40000 ALTER TABLE `waitlistentry` DISABLE KEYS */;
INSERT INTO `waitlistentry` VALUES (1,1,2,'ACTIVE','2026-11-12 17:28:38');
/*!40000 ALTER TABLE `waitlistentry` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-12 22:24:08
