U Smart Course Registration
D4 – Working Prototype / Release 0.5
This is our course registration and academic advising prototype for CSC 340. We built it using Java Spring Boot, MySQL, HTML, CSS, and JavaScript.

The goal is to make course registration easier for students and let advisors review registration requests. This is Release 0.5, so the main registration process is implemented, but some planned features are still unfinished.

GitHub: https://github.com/farah53738-cyber/AIU-Smart-Registration
Software needed
- Java JDK compatible with the project's pom.xml
- MySQL Server and MySQL Workbench
- Eclipse with Maven support (or another Java IDE)
- A web browser
How to run the project
1. Download the project or clone the GitHub repository.
2. Start MySQL and open MySQL Workbench.
3. Open database_setup.sql and run it to create the aiu_registration database and load sample data.
   Important: The setup script may drop and recreate existing tables. Only use it for initial setup or when you are okay with replacing the existing data.
4. In Eclipse, choose File → Import → Maven → Existing Maven Projects and select the folder containing pom.xml.
5. Open src/main/resources/application.properties and check the database connection settings. The current setup uses:
   - Database: aiu_registration
   - MySQL host and port: localhost:3306
   - Database user: root
   - Website port: 8082
6. In the Eclipse run configuration for AiuRegistrationApplication, set these environment variables:
   - DB_PASSWORD — your MySQL password
   - STUDENT_LOGIN_PASSWORD — password for the demo student accounts
   - ADVISOR_LOGIN_PASSWORD — password for the demo advisor account
7. Run AiuRegistrationApplication.java as a Spring Boot application.
8. When the console shows the application has started, open http://localhost:8082/login.
Do not put real passwords in the README or commit them to GitHub.
Demo accounts
Username	Role	Password
student1	Student	Value of STUDENT_LOGIN_PASSWORD
student2	Student	Value of STUDENT_LOGIN_PASSWORD
advisor	Advisor	Value of ADVISOR_LOGIN_PASSWORD


After a successful login, students are directed to the Student Dashboard and the advisor is directed to the Advisor Dashboard.
Running from Command Prompt
From the project folder, set the three environment variables first and then run:
mvnw.cmd spring-boot:run
Maven may need internet access the first time it downloads dependencies.
Website pages
Page	Address
Login	/login
Student Dashboard	/index.html
Advisor Dashboard	/advisor.html
Browse Courses	/courses.html
Register for Classes	/registration.html
My Registration Requests	/requests.html


Features implemented in Release 0.5
- Separate demo Student and Advisor accounts with role-based access
- Custom login page and dashboard redirects based on the account role
- Student and Advisor dashboards with navigation
- Course browsing and searching
- Viewing available course sections
- Students can submit registration requests
- Requests are saved in MySQL with an initial PENDING status
- Students can view requests linked to their demo student account
- Advisors can view registration requests and approve or reject pending requests
- Students can see the updated request status
- Checks for invalid sections and duplicate pending/approved requests for the same section
- CSRF protection for requests that change data
Features not finished yet
- Automatic enrollment and seat-count updates after approval
- Full prerequisite, academic hold, and schedule-conflict checks
- Semester planning and registration readiness features
- Complete advisor tools such as advisee management, schedules, and advising notes
- Real university account integration and production-ready user management
Current limitations
This is a prototype using demo accounts and sample course information, not a live university registration system. Student information is mapped to the demo accounts in the application.

Some dashboard cards are marked Coming soon and are not functional yet. Advisor approval changes a request's status, but does not automatically enroll the student or update available seats.

The database setup script is intended for initial setup and can overwrite existing project data. The project has mainly been developed for a local Windows and MySQL environment; other environments may need configuration changes.

The current sign-out links should be tested carefully. Redirecting to /login alone does not end a Spring Security session; a proper logout request is needed to switch accounts reliably.
Demo steps
1. Start MySQL and the Spring Boot application.
2. Log in as student1 and open the Student Dashboard.
3. Browse courses and choose a section.
4. Submit a registration request.
5. Open My Registration Requests and check that the new request appears as PENDING.
6. Log out completely and log in as advisor.
7. Open the Advisor Dashboard, find the pending request, and approve or reject it.
8. Log out, return as student1, and check the updated request status.
9. Repeat with another request if you want to demonstrate both approval and rejection.
These steps describe the intended demo workflow. Test the complete flow on the submitted version before recording your final demonstration.
Common problems
- Website does not open: Check that Spring Boot started successfully and use port 8082, not 8080.
- Database connection error: Check that MySQL is running, aiu_registration exists, and DB_PASSWORD is correct.
- Login fails: Check STUDENT_LOGIN_PASSWORD or ADVISOR_LOGIN_PASSWORD in your run configuration.
- Wrong account/dashboard appears: Log out completely, restart the application if needed, and sign in again.
- Old files appear: Make sure you are running the same project folder that you uploaded to GitHub. Save changes, restart the application, and refresh the browser.
