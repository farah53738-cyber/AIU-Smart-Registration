
CREATE TABLE IF NOT EXISTS `User`(user_id BIGINT AUTO_INCREMENT PRIMARY KEY,university_id VARCHAR(30) UNIQUE,name VARCHAR(100),password_hash VARCHAR(255),active BOOLEAN DEFAULT TRUE);
CREATE TABLE IF NOT EXISTS UserRole(user_id BIGINT,role VARCHAR(20),PRIMARY KEY(user_id,role));
CREATE TABLE IF NOT EXISTS StudentProfile(user_id BIGINT PRIMARY KEY,advisor_id BIGINT);
CREATE TABLE IF NOT EXISTS `Term`(term_id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(50),start_date DATE,end_date DATE,registration_open TIMESTAMP,registration_close TIMESTAMP);
CREATE TABLE IF NOT EXISTS course(course_id BIGINT AUTO_INCREMENT PRIMARY KEY,code VARCHAR(20) UNIQUE,title VARCHAR(100),credits INT,active BOOLEAN DEFAULT TRUE);
CREATE TABLE IF NOT EXISTS section(section_id BIGINT AUTO_INCREMENT PRIMARY KEY,course_id BIGINT,term_id BIGINT,instructor_id BIGINT,section_number VARCHAR(10),instructor VARCHAR(100),schedule VARCHAR(500),room VARCHAR(40),capacity INT,active BOOLEAN DEFAULT TRUE);
CREATE TABLE IF NOT EXISTS SectionMeeting(meeting_id BIGINT AUTO_INCREMENT PRIMARY KEY,section_id BIGINT,day_of_week INT,start_time TIME,end_time TIME,start_date DATE,end_date DATE,room VARCHAR(40));
CREATE TABLE IF NOT EXISTS CoursePrerequisite(course_id BIGINT,prerequisite_id BIGINT,PRIMARY KEY(course_id,prerequisite_id));
CREATE TABLE IF NOT EXISTS CompletedCourse(completion_id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT,course_id BIGINT,term_id BIGINT,passed BOOLEAN,grade_label VARCHAR(10),UNIQUE(student_id,course_id,term_id));
CREATE TABLE IF NOT EXISTS Plan(plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT,term_id BIGINT,version INT DEFAULT 0,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(student_id,term_id));
CREATE TABLE IF NOT EXISTS PlanItem(plan_id BIGINT,section_id BIGINT,PRIMARY KEY(plan_id,section_id));
CREATE TABLE IF NOT EXISTS registrationrequest(request_id BIGINT AUTO_INCREMENT PRIMARY KEY,student_name VARCHAR(100),student_id VARCHAR(30),section_id BIGINT,student_user_id BIGINT,term_id BIGINT,status VARCHAR(20),requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,decided_at TIMESTAMP,decided_by BIGINT,reason VARCHAR(1000),version INT DEFAULT 0,idempotency_key VARCHAR(64),UNIQUE(student_user_id,idempotency_key));
CREATE TABLE IF NOT EXISTS RequestItem(request_id BIGINT,section_id BIGINT,PRIMARY KEY(request_id,section_id));
CREATE TABLE IF NOT EXISTS Enrollment(enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT,section_id BIGINT,request_id BIGINT,status VARCHAR(20),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(student_id,section_id));
CREATE TABLE IF NOT EXISTS WaitlistEntry(waitlist_id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT,section_id BIGINT,status VARCHAR(20),joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UNIQUE(student_id,section_id));
CREATE TABLE IF NOT EXISTS OutboxEvent(event_id BIGINT AUTO_INCREMENT PRIMARY KEY,type VARCHAR(40),aggregate_id BIGINT,payload JSON,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,processed_at TIMESTAMP,attempts INT DEFAULT 0);
CREATE TABLE IF NOT EXISTS Notification(notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,event_id BIGINT,type VARCHAR(40),message VARCHAR(2000),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,read_at TIMESTAMP,UNIQUE(event_id,user_id));
CREATE TABLE IF NOT EXISTS AuditLog(audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_id BIGINT,action VARCHAR(60),entity_type VARCHAR(40),entity_id BIGINT,occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,request_id VARCHAR(64));
