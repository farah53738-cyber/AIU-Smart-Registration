AIU Smart Course Registration System
D5 – Complete Implementation / Beta Release
Course: CSC 340 – Software Engineering
Technology: Java, Spring Boot 4.1.2, MySQL 8, HTML, CSS, and JavaScript
GitHub: https://github.com/farah53738-cyber/AIU-Smart-Registration
Project overview
This is our course registration and academic advising system for CSC 340. The goal is to help students find classes, plan schedules, and register while allowing advisors and other university staff to manage registration information. This D5 Beta Release expands our D4 working prototype with more complete registration, enrollment, waitlist, notification, and role-based features. The system uses sample data and is intended for coursework rather than live university use.
Software needed
- Java JDK 17 or later (we developed with Java 23)
- MySQL Server 8 and MySQL Workbench
- Eclipse with Maven support, or another Java IDE
- A web browser
How to run the project
1. Download or clone the repository, then open the folder containing pom.xml (AIURegistrationD5Submission in the final ZIP).
2. Start MySQL and open MySQL Workbench. Create the database if it does not exist: CREATE DATABASE IF NOT EXISTS aiu_registration;
3. For a new installation, import the supplied D5 .sql database export using Server → Data Import → Import from Self-Contained File and select aiu_registration as the target schema. Back up existing data first: the export contains DROP TABLE IF EXISTS statements.
4. In Eclipse, select File → Import → Maven → Existing Maven Projects and choose the folder containing pom.xml.
5. Check src/main/resources/application.properties. The setup uses MySQL at localhost:3306, database aiu_registration, user root, and web port 8082.
6. In the Eclipse Run Configuration's Environment tab, configure:
   - DB_PASSWORD: your MySQL password
   - STUDENT_LOGIN_PASSWORD: student demo-account initialization password
   - ADVISOR_LOGIN_PASSWORD: advisor/staff demo-account initialization password
7. Run AiuRegistrationApplication.java as a Spring Boot application.
8. Visit http://localhost:8082/login.
Important: The two demo login password variables initialize only accounts that still have placeholder password hashes. They do not reset passwords already stored in an imported database. Never commit real passwords to GitHub. On Windows, you can also run mvnw.cmd spring-boot:run after setting the environment variables.
Demo accounts
Username	Role
student1	Student
student2	Student
advisor	Academic Advisor
instructor_ahmed	Instructor
registrar	Registrar
admin	Administrator


Additional instructor accounts are also present in the demo database. Use the configured local demo passwords. Students are directed to the Student Dashboard; staff accounts use the role-based academic portal.
Main pages
Page	Address
Login	/login
Student Dashboard	/index.html
Academic Portal	/portal.html
Browse Courses	/portal.html#catalog
Register for Classes	/portal.html#register
My Schedule	/portal.html#schedule
My Requests / Advisor Review	/portal.html#requests
My Waitlists	/portal.html#waitlists
Notifications	/portal.html#inbox


Some older page addresses, such as /advisor.html and /registration.html, now redirect to views in the academic portal. Available views depend on the signed-in role.
Features implemented in D5
Students: Course browsing and search; section information and unique class numbers; saved semester plans; schedule display and conflict detection; prerequisite and registration validation; registration requests; approved enrollments; request withdrawal (hidden from the active list but retained in database history); waitlist join/leave; notification inbox.

Academic Advisors: Review assigned students' registration requests; approve or reject with a reason; view assigned students; directly register an eligible assigned student by class number, subject to validation and available seats.

Instructors: View assigned courses, sections, and class rosters.

Registrar: Manage courses, sections, meeting schedules, terms, prerequisites, student records, enrollments, and registration notices.

Administrator: Manage user accounts and roles, courses, and sections.

System-wide: MySQL database operations, role-based permissions, password-hash authentication, CSRF protection, server-side validation, error handling, and audit records.
Current limitations
- D5 is a beta, not a production system or a live university integration.
- The Registrar/Administrator Reports navigation link was intentionally removed. Report-related backend functionality remains, but the visible interface does not fully demonstrate the administrative-reports requirement (FR-14).
- The SQL file contains demonstration accounts and historical test data; some stored names or notifications may still reflect testing.
- Java/Maven integration tests and full end-to-end MySQL workflows need to be verified on the local development machine. Passing simulated frontend checks alone does not establish that every use case works.
- Production features such as university single sign-on, deployment hardening, and external messaging are not included.
Suggested demo
1. Start MySQL and Spring Boot, then sign in as student1.
2. Browse courses, choose a class number, and submit a registration request.
3. Sign out and log in as advisor; review and approve or reject the request.
4. Return as the student and show the request status, confirmed schedule, and notification.
5. Demonstrate a waitlist and the advisor's Register a Student option.
6. Sign in as an instructor to show assigned classes and rosters.
7. Sign in as Registrar and Administrator to demonstrate their management pages and role restrictions.
These steps are a recommended workflow; verify them with suitable demo data before recording.
Common problems
- Website does not open: Check Spring Boot startup and use port 8082, not 8080.
- Database connection error: Check MySQL, aiu_registration, and DB_PASSWORD.
- Login fails: Check the stored demo-account password. Environment variables do not overwrite existing hashed passwords.
- Wrong user/dashboard: Use Sign out to end the Spring Security session, then log in again.
- Old files appear: Check that Eclipse runs the latest project, restart, and refresh with Ctrl + F5.

