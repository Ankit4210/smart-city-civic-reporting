# Software Development Life Cycle (SDLC)
## Phase 2: System Architecture, UML Modeling & Data Flow Diagrams (DFD)

**Project Title:** Safe & Smart City – Civic Issue Reporting & Management System  
**Academic Level:** B.Sc. Computer Science / Engineering Mini Project  

---

### 1. High-Level 3-Tier System Architecture

The application adopts a decoupled 3-tier architecture ensuring scalability, separation of concerns, and security.

```mermaid
graph TB
    subgraph Client_Layer [Presentation Layer - Frontend]
        Browser[Modern Web Browser - Mobile/Desktop]
        LeafletUI[Leaflet.js Interactive GIS Map]
        ChartUI[Chart.js Municipal Analytics]
        AuthUI[Bearer Token Session & Auth Forms]
    end

    subgraph Server_Layer [Application Layer - Java Servlet REST API]
        Router[Jakarta Servlet API Router]
        AuthMiddleware[Signed Bearer Token & Role Validator]
        UploadHandler[Servlet Multipart File Handler]
        HotspotEngine[GIS Density Clustering Engine]
        IssueController[Servlets & JDBC DAOs]
        AdminController[Admin Triage & Resolution]
    end

    subgraph Data_Layer [Data & Storage Layer]
        MySQLDB[(MySQL 8 Relational Database)]
        UploadsDir[File System - /uploads Before/After Proofs]
    end

    Browser -->|HTTP/REST /api/*| Router
    Router --> AuthMiddleware
    Router --> UploadHandler
    UploadHandler -->|Saves Images| UploadsDir
    AuthMiddleware --> IssueController
    AuthMiddleware --> AdminController
    AdminController --> HotspotEngine
    IssueController --> MySQLDB
    AdminController --> MySQLDB
    LeafletUI -.->|Renders Coordinates| IssueController
    ChartUI -.->|Fetches Metrics| AdminController
```

---

### 2. UML Use Case Diagram

```mermaid
graph LR
    subgraph UseCases [Smart City Civic Portal]
        UC1(Register & Login)
        UC2(Report Issue with GPS & Photo)
        UC3(Track Status via Tracking ID)
        UC4(Upvote Community Issue)
        UC5(Provide Resolution Feedback)
        UC6(Triage & Dispatch Field Crews)
        UC7(Upload After-Photo & Mark Resolved)
        UC8(Inspect GIS Problem Hotspots)
        UC9(View Municipal Analytics Charts)
    end

    Citizen((Citizen User)) --> UC1
    Citizen --> UC2
    Citizen --> UC3
    Citizen --> UC4
    Citizen --> UC5

    Admin((Municipal Admin)) --> UC1
    Admin --> UC3
    Admin --> UC6
    Admin --> UC7
    Admin --> UC8
    Admin --> UC9

    Guest((Public / Guest)) --> UC3
```

---

### 3. UML Sequence Diagram: End-to-End Civic Reporting & Resolution Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Citizen as Citizen (Ankit)
    participant Client as Web App (Frontend)
    participant Server as Java Servlet API
    participant Storage as File Storage (/uploads)
    participant DB as MySQL Database
    actor Admin as Municipal Admin (Sharma)

    Citizen->>Client: 1. Click 'Report Issue' + Capture Live GPS
    Client->>Citizen: 2. Lock Coordinates (Lat: 28.6139, Lng: 77.2090)
    Citizen->>Client: 3. Select Category, Enter Description, Upload Before-Photo
    Client->>Server: 4. POST /api/issues (FormData + bearer token)
    Server->>Storage: 5. Store Before-Photo (evidence-xxx.jpg)
    Server->>DB: 6. INSERT into reports (Tracking ID: SSC-2026-XXXX, Status: Pending)
    Server->>DB: 7. UPDATE users SET civic_points = civic_points + 50
    Server-->>Client: 8. HTTP 201 Created (Tracking ID + +50 Pts)
    Client-->>Citizen: 9. Display Tracking Receipt & Add Map Pin

    Admin->>Client: 10. Open Admin Console -> Filter Pending Issues
    Client->>Server: 11. GET /api/issues?status=Pending
    Server->>DB: 12. SELECT * FROM reports WHERE status = 'Pending'
    Server-->>Client: 13. Return Pending Issues List
    Admin->>Client: 14. Click 'Start Work' -> Enter Dispatch Remarks
    Client->>Server: 15. PATCH /api/admin/issues/:id/status (In Progress)
    Server->>DB: 16. UPDATE reports SET status = 'In Progress'
    Server->>DB: 17. INSERT into notifications (Citizen notified)

    Admin->>Client: 18. Work Completed -> Upload After-Photo Proof
    Client->>Server: 19. POST /api/admin/issues/:id/resolve (After-Photo + Remarks)
    Server->>Storage: 20. Store After-Photo (evidence-res-xxx.jpg)
    Server->>DB: 21. INSERT into resolutions & UPDATE reports SET status = 'Resolved'
    Server->>DB: 22. INSERT into notifications (Citizen alert)
    Server-->>Client: 23. Resolution Confirmed
    
    Citizen->>Client: 24. Inspect Stepper -> View Side-by-Side Proof
    Citizen->>Client: 25. Click 'Satisfied' (5 Stars)
    Client->>Server: 26. POST /api/issues/:id/feedback (Satisfied = true)
    Server->>DB: 27. INSERT into feedback & Award +20 Civic Points
```

---

### 4. Data Flow Diagrams (DFD)

#### 4.1 DFD Level 0 (Context Diagram)

```mermaid
graph LR
    Citizen[Citizen User] -->|1. Credentials, Complaint Data, Geolocation, Photos, Upvotes| System[0.0 Safe & Smart City Civic Issue System]
    System -->|2. Tracking ID, Status Updates, Proof Photos, Civic Points| Citizen

    Admin[Municipal Admin] -->|3. Status Updates, Dispatch Remarks, After-Photo Proofs| System
    System -->|4. GIS Hotspot Clusters, Complaint Triage, Analytical Metrics| Admin
```

#### 4.2 DFD Level 1 (Decomposition Diagram)

```mermaid
graph TD
    User[Citizen / Admin] -->|1. Credentials| P1[1.0 Authentication & Session Control]
    P1 -->|Store / Validate| D1[(D1: Users Store)]

    User -->|2. Report Data + GPS + Photo| P2[2.0 Complaint Ingestion & Tracking Engine]
    P2 -->|Save Report & Photo Path| D2[(D2: Reports & Uploads Store)]
    P2 -->|Increment Points| D1

    D2 -->|3. Active Complaints Data| P3[3.0 GIS Clustering & Hotspot Detection]
    P3 -->|High-Density Alerts| Admin[Municipal Admin]

    Admin -->|4. After-Photo & Remarks| P4[4.0 Resolution Proof & Triage Workflow]
    P4 -->|Update Status & Proof| D3[(D3: Resolutions Store)]
    P4 -->|Trigger Alert| P5[5.0 Notification & Feedback Engine]

    P5 -->|Push Notification| User
    User -->|5. Satisfaction Rating| P5
    P5 -->|Store Feedback| D4[(D4: Feedback Store)]
```

---

### 5. Summary of Phase 2 Deliverables
The System Design phase provides full architectural blueprinting, interaction modeling, and data flow pipelines, establishing the structural basis for database implementation and coding.
