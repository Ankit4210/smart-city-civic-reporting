# Safe & Smart City Java Backend

This folder builds a complete, self-contained Java 17, Jakarta Servlet 6, JDBC, and MySQL website for the civic issue reporting synopsis. Maven packages the existing website from `../public` and its demo photos from `../uploads` into the WAR. Tomcat serves the website and JSON API from the same origin; no Node.js server or separate frontend host is needed.

## Requirements

- JDK 17+
- Apache Tomcat 10.1+
- MySQL 8.0+
- Maven 3.8+

Create the database once:

```sql
CREATE DATABASE smart_city_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The application creates its tables and seeds issue categories at startup. If the users table is empty, it also creates demo accounts:

- Admin: `admin@example.test` / `admin123`
- Citizen: `citizen.one@example.test` / `citizen123`

These credentials are for local demonstrations only. Change them before using a shared or public deployment. Do not run the root `database.sql` script against existing data: that script drops and recreates its tables.

Login works only while the Java/Tomcat API and MySQL database are running. GitHub Pages does not host the API. If using the repository's SQL sample data, the demo hashes are configured for the passwords listed above; local application startup also repairs the previously published placeholder hash without changing users' own passwords.

## GitHub Pages with an online backend

GitHub Pages cannot run this Java servlet API or MySQL. To use real registration and login from Pages, deploy the backend and MySQL separately (the repository includes a Railway-ready `Dockerfile` and `railway.json`), then set `SMARTCITY_API_BASE_URL` in both `js/api-config.js` and `public/js/api-config.js` to the backend's public origin without `/api`.

Set `SMARTCITY_CORS_ALLOWED_ORIGINS` on the backend to the exact Pages origin, for example `https://ankit4210.github.io`. Railway MySQL services expose `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, and `MYSQLPASSWORD`; the backend uses those to construct its JDBC connection when `SMARTCITY_DB_URL` is unset.

For a public deployment, keep `SMARTCITY_ENV=production`, configure `SMARTCITY_ADMIN_EMAIL`, a private `SMARTCITY_ADMIN_PASSWORD` of at least 16 characters, and a private `SMARTCITY_AUTH_SECRET` of at least 32 characters. Production mode disables the local demo accounts and seeds the configured administrator only when the users table is empty. Keep every secret in the hosting provider's environment-variable settings, never in the frontend or repository.

Uploaded photos are stored in Tomcat's deployed web application directory. Configure persistent storage for `/usr/local/tomcat/webapps/ROOT/uploads` on the hosting provider if uploaded evidence must survive container replacement.

Configure the connection before starting Tomcat. Environment variables override the defaults in `src/main/webapp/WEB-INF/web.xml`:

```powershell
$env:SMARTCITY_DB_URL = 'jdbc:mysql://localhost:3306/smart_city_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true'
$env:SMARTCITY_DB_USER = 'root'
$env:SMARTCITY_DB_PASSWORD = 'your-mysql-password'
```

Build and deploy:

```powershell
mvn clean package
Copy-Item target\SmartCity.war C:\path\to\tomcat\webapps\
```

Open `http://localhost:8080/SmartCity/` after Tomcat starts. Uploaded JPEG, PNG, and WebP photos (up to 25 MB each) are stored in the deployed web application's `/uploads` directory. Keep that directory writable by Tomcat and include it in deployment backups. Browser live location requires the user to allow location access and the site to run in a secure context (HTTPS or localhost); if permission is unavailable, select the location on the map.

For repeatable logins across Tomcat restarts or multiple application instances, set `SMARTCITY_AUTH_SECRET` to a private random value with at least 32 characters. If omitted, a secure random secret is generated for that application process and existing login tokens expire when the application restarts.

## API

Successful responses use JSON and include `success: true`; errors include `success: false` and an HTTP error status. Protected routes require `Authorization: Bearer <token>`. Login and registration tokens expire after eight hours and are invalidated when the application restarts.

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Register a citizen with JSON `name`, `email`, and `password`; optional `phone`, `ward` |
| POST | `/api/auth/login` | Public | Authenticate with JSON `email` and `password` |
| GET | `/api/auth/me` | Citizen | Return the current account |
| GET | `/api/categories` | Public | List report categories |
| GET | `/api/stats` | Public | Show public issue and citizen totals |
| GET | `/api/issues` | Public | Browse/filter reports (`category_id`, `status`, `ward`, `search`, `sort`) |
| POST | `/api/issues` | Citizen | Multipart report (`category_id`, `title`, `description`, `latitude`, `longitude`, `photo`; optional `landmark`, `address`, `ward`, `priority`) |
| GET | `/api/issues/my-reports` | Citizen | List the current citizen's reports |
| GET | `/api/issues/{id}` | Public | Get report details |
| POST | `/api/issues/{id}/upvote` | Citizen | Toggle one community upvote |
| POST | `/api/issues/{id}/feedback` | Report owner | Rate a resolved report (`rating`, `is_satisfied`, optional `comments`); dissatisfaction reopens it |
| GET | `/api/track/{trackingId}` | Public | Track a report and view resolution proof |
| GET | `/api/admin/dashboard` | Admin | Filtered report list and dashboard counts |
| PATCH | `/api/admin/issues/{id}/status` | Admin | Change a report to `Pending` or `In Progress`; optional `remarks`, `priority` |
| POST | `/api/admin/issues/{id}/resolve` | Admin | Multipart resolution (`after_photo`; optional `remarks`, `action_taken`) |
| GET | `/api/admin/hotspots` | Public | List areas with multiple nearby active reports of the same category |
| GET | `/api/admin/analytics` | Admin | Ward, category, and status counts; hotspots and average resolution time by category |
| GET | `/api/notifications` | Citizen | List notifications and unread count |
| PATCH | `/api/notifications/{id}/read` | Citizen | Mark one notification as read |
| PATCH | `/api/notifications/read-all` | Citizen | Mark all notifications as read |
| GET | `/api/leaderboard` | Public | List the top ten citizen contributors |

Report photos must be actual JPEG, PNG, or WebP files. When a submitted report matches an active report in the same ward/category within 500 metres, the existing report is returned instead of inserting a duplicate. A citizen without an existing vote adds one upvote, earns the upvote points, and triggers a notification for the reporter; the report is elevated to high priority. The tracking modal refreshes the current report from the API every 10 seconds while open.
