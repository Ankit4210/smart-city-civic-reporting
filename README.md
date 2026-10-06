# Safe & Smart City - Civic Issue Reporting System

Java Servlet/JDBC/MySQL civic issue reporting website built to match the project synopsis. The existing HTML, CSS, and JavaScript frontend is packaged with the API as one Tomcat WAR.

## Requirements

- JDK 17 or newer
- Apache Tomcat 10.1 or newer
- MySQL 8.0 or newer
- Maven 3.8 or newer

## Run locally

Create the database in MySQL:

```sql
CREATE DATABASE smart_city_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Set the connection settings in PowerShell before starting Tomcat:

```powershell
$env:SMARTCITY_DB_URL = 'jdbc:mysql://localhost:3306/smart_city_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true'
$env:SMARTCITY_DB_USER = 'root'
$env:SMARTCITY_DB_PASSWORD = 'your-mysql-password'
```

Build and deploy:

```powershell
cd SmartCityJava
mvn clean package
Copy-Item .\target\SmartCity.war C:\path\to\tomcat\webapps\
```

Start Tomcat and open `http://localhost:8080/SmartCity/`. On first startup, the application creates the required tables and seeds categories and local demonstration accounts if the users table is empty.

Fictional demo accounts for local use only (change these credentials before any shared deployment):

- Admin: `admin@example.test` / `admin123`
- Citizen: `citizen.one@example.test` / `citizen123`

Set `SMARTCITY_AUTH_SECRET` to a private random value of at least 32 characters for stable tokens across restarts. Change demo passwords before any shared deployment. Keep the deployed `uploads` directory writable and backed up.

## Project layout

- `SmartCityJava/` - Java 17 Servlet API, JDBC DAOs, MySQL initialization, Maven WAR, and backend setup documentation.
- `public/` - Frontend files packaged into the WAR.
- `css/`, `js/`, `index.html` - Matching root-level frontend copies for static preview.
- `uploads/` - Sample photos included in the WAR; uploaded evidence is stored by the deployed application.
- `database.sql` - Reference SQL schema; review it before use because it may recreate existing tables.
- `docs/` and `PROJECT_REPORT.md` - Academic SDLC and project documentation.
- `Smart_City_Civic_Issue_Reporting_Synopsis.docx` - Original project synopsis.

See [SmartCityJava/README.md](./SmartCityJava/README.md) for database configuration and the API reference.

## Verification

Build the backend and WAR with:

```powershell
mvn -f .\SmartCityJava\pom.xml clean package
```

The WAR build verifies Java compilation and packages the UI; validating live login, report submission, and admin workflows also requires a configured MySQL server and Tomcat instance.
