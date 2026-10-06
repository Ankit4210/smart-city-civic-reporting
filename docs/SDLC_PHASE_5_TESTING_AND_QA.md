# Software Development Life Cycle (SDLC)
## Phase 5: Software Testing, Quality Assurance & Test Case Matrix

**Project Title:** Safe & Smart City – Civic Issue Reporting & Management System  
**Academic Level:** B.Sc. Computer Science Mini Project

---

### 1. Verification status

The Java 17 Maven WAR build passed. The packaged application was deployed to a temporary, loopback-only Apache Tomcat 10.1.60 instance backed by an isolated MySQL 8.0.46 database on October 7, 2026. Live HTTP, JDBC schema creation, authentication, reporting, duplicate matching, upvoting, administration, resolution, feedback, notification, analytics, and browser tracking-refresh flows were exercised.

Live deployment exposed a JDBC driver auto-discovery failure in the webapp classloader. `DBUtil` now explicitly loads the MySQL Connector/J driver; the rebuilt WAR started, initialized all eight tables, and served the tested endpoints successfully.

### 2. Test cases

| ID | Module | Scenario | Expected result |
| --- | --- | --- | --- |
| TC-01 | Registration | Register with valid citizen details | Account created and bearer token returned |
| TC-02 | Registration | Register with an email already in use | Clear client error; no duplicate account |
| TC-03 | Login | Log in with correct credentials | Account profile and bearer token returned |
| TC-04 | Login | Log in with incorrect credentials | Authentication rejected |
| TC-05 | Geolocation | Grant browser location permission | Valid latitude and longitude populate the form |
| TC-06 | Geolocation | Select or drag a map marker | Coordinates update to the selected location |
| TC-07 | Report | Submit a report with required details and photo | Report saved with a unique tracking ID |
| TC-08 | Report | Submit without required title or description | Form validation blocks submission |
| TC-09 | Report | Submit without a photo or with an oversized/unsupported file | Form rejects the upload and explains the constraint |
| TC-10 | Tracking | Track an existing tracking ID | Current status, evidence, and timeline are shown |
| TC-11 | Tracking | Track an unknown tracking ID | Not-found message is shown |
| TC-12 | Community feed | Filter by category | Only matching reports are displayed |
| TC-13 | Community feed | Filter by ward | Only matching reports are displayed |
| TC-14 | Upvoting | Upvote a report as a citizen | Vote state and count reflect the saved change |
| TC-15 | Upvoting | Toggle an existing vote off | Vote is removed and count is updated |
| TC-16 | Duplicate prevention | Submit within 500 m of an active same-ward/category issue | Existing report is returned; no duplicate row is inserted |
| TC-17 | Admin | Open admin dashboard as an administrator | Dashboard statistics and triage list load |
| TC-18 | Authorization | Call an admin endpoint as a citizen | Request is rejected with forbidden status |
| TC-19 | Triage | Change a pending report to in progress | Status and citizen notification are persisted |
| TC-20 | Resolution | Resolve a report with after-photo proof | Resolution, status, and notification are persisted |
| TC-21 | Feedback | Submit satisfied feedback as the report owner | Feedback is stored and shown in tracking details |
| TC-22 | Feedback | Submit unsatisfied feedback as the report owner | Feedback is stored and the report is reopened |
| TC-23 | Hotspots | Load admin hotspot analytics | Active ward/category clusters are returned |
| TC-24 | Analytics | Load dashboard charts | Category and status datasets match database counts |
| TC-25 | Notifications | Mark one or all notifications read | Read state and unread badge count update |
| TC-26 | Duplicate upvote | Match an active report as a citizen without an existing vote | Existing issue gains one upvote, priority is raised, points and reporter notification are stored |
| TC-27 | Live tracking | Keep a tracking modal open while an administrator changes the issue | New status/proof appears within 10 seconds without disturbing unchanged modal content |

### 3. Execution record

| ID | Result | Observed verification |
| --- | --- | --- |
| TC-01 | PASS | Registered a new citizen and received a token. |
| TC-02 | PASS | Duplicate registration returned HTTP 409. |
| TC-03 | PASS | Seeded citizen/admin and newly registered citizen logins succeeded. |
| TC-04 | PASS | Incorrect password returned HTTP 401. |
| TC-05 | PARTIAL | Coordinate fields populated, but the browser verification exercised the simulated fallback; a real location-permission grant and device GPS fix were not verified. |
| TC-06 | PASS | Clicking the Leaflet map changed the report coordinates. |
| TC-07 | PASS | Multipart report and PNG evidence were stored with a tracking ID. |
| TC-08 | PASS | Omitting the required report title returned HTTP 400. |
| TC-09 | PARTIAL | Missing and unsupported photo uploads returned HTTP 400; an upload above 10 MB was not exercised. |
| TC-10 | PASS | Existing tracking returned status, photo evidence, and the audit timeline. |
| TC-11 | PASS | Unknown tracking ID returned HTTP 404. |
| TC-12 | PASS | Category-filtered feed contained only the requested category. |
| TC-13 | PASS | Ward-filtered feed contained only the requested ward. |
| TC-14 | PASS | Citizen upvote increased the count and recorded the vote. |
| TC-15 | PASS | Repeating the upvote toggled it off. |
| TC-16 | PASS | Same ward/category within 500 m reused the active report; just-over-500 m and different ward/category submissions did not. |
| TC-17 | PASS | Administrator dashboard returned statistics and issue data. |
| TC-18 | PASS | Citizen access to admin data returned HTTP 403; anonymous access returned HTTP 401. |
| TC-19 | PASS | Administrator status transition succeeded and was recorded in the report timeline. |
| TC-20 | PASS | Administrator resolution accepted after-photo evidence and tracking showed the resolution. |
| TC-21 | PASS | Satisfied owner feedback was stored without reopening the resolved report. |
| TC-22 | PASS | Unsatisfied owner feedback was stored and reopened the report to `In Progress`. |
| TC-23 | PASS | Hotspot endpoint returned successfully. |
| TC-24 | PASS | Analytics endpoint returned datasets; status totals matched the live database counts. |
| TC-25 | PASS | Mark-all-read changed the citizen unread count to zero. |
| TC-26 | PASS | Duplicate support added exactly one upvote and 10 points, raised priority to High, and notified the original reporter; repeat submission added no second vote. |
| TC-27 | PASS | With the tracking modal open, an admin status change appeared in its timeline on the next 10-second refresh. |

The MySQL data directory and Tomcat deployment used for these checks were temporary and isolated from project data and Windows services. Maven reports no automated Java test sources. This execution record is not a production load, concurrency, browser compatibility, or security certification; the over-10-MB upload boundary remains unverified.
