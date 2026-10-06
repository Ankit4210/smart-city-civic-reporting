# Software Development Life Cycle (SDLC)
## Phase 4: Implementation & Algorithms

**Project Title:** Safe & Smart City – Civic Issue Reporting & Management System  
**Academic Level:** B.Sc. Computer Science / Engineering Mini Project  

---

### 1. Technology Stack Selection Rationale

| Layer | Selected Technology | Technical Rationale |
| :--- | :--- | :--- |
| **Frontend UI** | HTML5, CSS3 Glassmorphism, Vanilla JS (ES6+) | Ultra-fast execution, zero build pipeline dependencies, native DOM responsiveness, lightweight. |
| **Mapping Engine** | Leaflet.js & OpenStreetMap | Open-source, no billing or API key restrictions, fluid mobile pinch-to-zoom and draggable pins. |
| **Data Visualization** | Chart.js 4.x | Canvas-rendered hardware-accelerated interactive doughnut and bar charts for municipal analytics. |
| **Backend Runtime** | Java 17, Jakarta Servlet 6, Apache Tomcat 10.1+ | Servlet-based REST endpoints using the synopsis technology stack. |
| **Data Access** | JDBC and MySQL Connector/J | Parameterized SQL queries against the live MySQL database. |
| **File Handling** | Servlet multipart upload API | Validated JPEG, PNG, and WebP image uploads with a 25 MB per-photo limit. |
| **Authentication** | BCrypt passwords and signed expiring bearer tokens | Role checks on protected API routes; token signing key can be configured outside source code. |
| **Database Engine** | MySQL 8.0+ | Persistent relational storage; application startup initializes the required tables and categories. |

---

### 2. Codebase Organization

```
project/
├── SmartCityJava/
│   ├── pom.xml                    # Java 17 WAR build and dependencies
│   ├── src/main/java/             # Servlets, JDBC DAOs, models, and utilities
│   └── src/main/webapp/WEB-INF/   # Tomcat servlet configuration
├── public/                        # Frontend packaged into the WAR
├── uploads/                       # Sample photos packaged into the WAR
├── database.sql                   # Reference MySQL schema
└── docs/                          # SDLC academic documentation
```

---

### 3. Core Algorithms & Logic

#### 3.1 Tracking ID Generation Algorithm
To provide a collision-free, human-readable complaint reference, the system generates IDs conforming to the pattern:
$$\text{Tracking ID} = \text{"SSC-"} + \text{Year} + \text{"-"} + \text{RandomInteger}(1000, 9999)$$

The Java DAO generates the year-tagged identifier and relies on the database uniqueness constraint; if a collision occurs, the insert fails rather than silently assigning a duplicate.

#### 3.2 GIS Hotspot Clustering Algorithm
The system analyzes active (unresolved) complaints by grouping coordinates within the same municipal ward and problem category to detect high-density problem clusters:

$$\text{Hotspot Density} = \sum \text{Reports where } (\text{status} \neq \text{'Resolved'}) \text{ GROUP BY ward, category}$$

```sql
SELECT 
  r.ward,
  c.name as category_name,
  c.icon as category_icon,
  AVG(r.latitude) as center_lat,
  AVG(r.longitude) as center_lng,
  COUNT(r.id) as issue_count,
  SUM(CASE WHEN r.priority = 'Critical' THEN 1 ELSE 0 END) as critical_count
FROM reports r
JOIN categories c ON r.category_id = c.id
WHERE r.status != 'Resolved'
GROUP BY r.ward, r.category_id
HAVING COUNT(r.id) >= 1
ORDER BY issue_count DESC, critical_count DESC;
```

#### 3.3 Civic Karma Points Computation Logic
The gamification engine calculates dynamic score increments to reward genuine civic participation:
- $\Delta \text{Points}_{\text{Report}} = +50$ upon complaint submission.
- $\Delta \text{Points}_{\text{Upvote}} = +10$ upon validating a community issue.
- $\Delta \text{Points}_{\text{Resolved}} = +25$ when a citizen's complaint is officially resolved.
- $\Delta \text{Points}_{\text{Feedback}} = +20$ when a citizen provides resolution satisfaction feedback.

---

### 4. REST API Endpoint Specification

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Registers a new citizen account and awards 50 points |
| `POST` | `/api/auth/login` | Public | Authenticates citizen or admin and returns a signed bearer token |
| `GET` | `/api/auth/me` | Authenticated | Fetches current user profile and activity counters |
| `GET` | `/api/categories` | Public | Lists all civic categories, SLA hours, and departments |
| `GET` | `/api/issues` | Public/Optional | Lists issues with category, status, ward, search filters |
| `POST` | `/api/issues` | Authenticated | Uploads before-photo, stores GPS coordinates, creates issue |
| `GET` | `/api/issues/my-reports`| Authenticated | Retrieves reports filed by the logged-in citizen |
| `GET` | `/api/track/:trackingId`| Public | Fetches issue details, stepper status, before/after proof |
| `POST` | `/api/issues/:id/upvote`| Authenticated | Toggles upvote on an issue (+10 points) |
| `POST` | `/api/issues/:id/feedback`| Authenticated | Records satisfaction rating; reopens issue if unsatisfied |
| `GET` | `/api/admin/dashboard` | Admin Only | Retrieves high-level KPI metrics |
| `PATCH`| `/api/admin/issues/:id/status`| Admin Only | Changes status to 'In Progress' with dispatch remarks |
| `POST` | `/api/admin/issues/:id/resolve`| Admin Only | Uploads After-Photo proof, remarks, and marks Resolved |
| `GET` | `/api/admin/hotspots` | Public | Computes GIS high-density problem clusters |
| `GET` | `/api/admin/analytics` | Admin | Returns area, category, and status data for Chart.js |
| `GET` | `/api/leaderboard` | Public | Returns Top 10 citizens ranked by Civic Points |

---

### 5. Summary of Phase 4 Deliverables
The implementation provides the Servlet REST API, JDBC data access, bearer-token authentication, photo upload handling, and database-backed civic workflows. A production deployment still requires environment-specific configuration and live operational testing.
