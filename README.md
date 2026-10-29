# AIU Smart Course Registration
## D4 – Working Prototype / Release 0.5

This is our course registration prototype for CSC 340. We used Java Spring Boot for the backend, MySQL for the database, and HTML, CSS, and JavaScript for the website.

So far, users can browse courses, choose a section, submit a registration request, and view saved requests.

GitHub repository:
https://github.com/farah53738-cyber/AIU-Smart-Registration

## Software needed

- JDK 17 or a compatible newer version
- MySQL Server 8.0 and MySQL Workbench
- Eclipse with Maven support
- A web browser

## How to run the project

1. Download and extract the project, or clone the GitHub repository.
2. Open MySQL Workbench and connect to your local MySQL server.
3. Go to File → Open SQL Script and open `database_setup.sql`.
4. Run the script to create the database and load the sample data.

**Important:** This script replaces the existing project tables and their data. Use it for the initial setup, not every time you run the website.

5. In Eclipse, select File → Import → Maven → Existing Maven Projects.
6. Choose the project folder containing `pom.xml` and finish the import.
7. Open `src/main/resources/application.properties` and check these settings:

   - Database: `aiu_registration`
   - MySQL port: `3306`
   - Database username: `root`
   - Website port: `8080`

   Change the database username or connection settings if your computer uses different ones.

8. Open Run → Run Configurations and select the configuration for `AiuRegistrationApplication`. If it is not listed, run the Java class once to create it.
9. In the Environment tab, add:

   - `DB_PASSWORD`: your local MySQL password
   - `APP_LOGIN_PASSWORD`: a password you choose for the website login

   Leave the password placeholders in `application.properties`. Actual passwords should not be uploaded to GitHub.

10. Run `AiuRegistrationApplication.java`.
11. Once the Console shows that the application has started, open:

    http://localhost:8080/

12. Log in with username `admin` and the password you set for `APP_LOGIN_PASSWORD`.

Keep the application running while using the website.

### Running through Command Prompt

You can also run the project from its main folder using:

```bat
mvnw.cmd spring-boot:run
```

Set `DB_PASSWORD` and `APP_LOGIN_PASSWORD` in the same Command Prompt session first. Internet access is needed to download Maven dependencies on the first run.

## Website pages

| Page | Address |
|---|---|
| Homepage | `/` |
| Browse Courses | `/courses.html` |
| Register for Classes | `/registration.html` |
| Registration Requests | `/requests.html` |

Open the pages through the running website so they can communicate with the backend.

## What works

- Login using a shared demo account
- Navigation between the working pages
- Browsing and searching courses
- Selecting a section and submitting a request
- Checking student details and whether the section exists
- Saving requests in MySQL with a `PENDING` status
- Viewing and refreshing saved requests
- Using parameterized SQL for inserts and a CSRF token when submitting

The database includes eight sample courses, eight sections, and one sample registration request.

## What is not finished

- Separate student and advisor accounts
- Showing only the logged-in student's requests
- Advisor approval and rejection
- Updating enrollment and available seats
- Planning future semesters
- Checking prerequisites, holds, and timetable conflicts

## Current limitations

The `admin` account is only a shared demo login. It does not have a working admin dashboard.

The requests page currently displays all submitted requests. Requests stay `PENDING` because the advisor decision feature is not implemented yet.

The course sections, instructors, rooms, and schedules are sample data. The database is a simplified version of the planned system. Duplicate requests and full sections are not blocked yet.

We have run the project on Windows. The exported table names are lowercase, while some Java queries use capital letters. These names may need to be matched before running on a case-sensitive MySQL installation.

## Demo steps

1. Log in.
2. Browse courses and try the search.
3. Open the registration form.
4. Enter demo student details and select a section.
5. Submit the request.
6. Open View Requests and check that it appears as `PENDING`.
7. Refresh the list.

Registration submission and request display have been demonstrated. Before submission, we still need to check installation on another computer and confirm that requests remain after restarting the application.

## Common problems

- **Port 8080 is already in use:** Stop the earlier running application. You can also change `server.port` to `8081` and open http://localhost:8081/.
- **Database connection fails:** Check that MySQL is running and that the database settings and password are correct.
- **Login fails:** Check the `APP_LOGIN_PASSWORD` value in the run configuration.
- **Old page design appears:** Save the files, refresh the Eclipse project, restart the application, and press Ctrl + F5 in the browser.
