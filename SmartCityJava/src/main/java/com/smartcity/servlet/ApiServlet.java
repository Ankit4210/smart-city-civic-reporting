package com.smartcity.servlet;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartcity.dao.CategoryDAO;
import com.smartcity.dao.ReportDAO;
import com.smartcity.dao.UserDAO;
import com.smartcity.model.Report;
import com.smartcity.model.User;
import com.smartcity.util.AuthTokenUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 25L * 1024 * 1024,
        maxRequestSize = 27L * 1024 * 1024
)
public class ApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final long MAX_PHOTO_SIZE_BYTES = 25L * 1024 * 1024;
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create();

    private final UserDAO userDAO = new UserDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");

        if (!configureCors(request, response)) {
            return;
        }

        try {
            route(request, response);
        } catch (PhotoTooLargeException e) {
            sendError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Photo must be 25 MB or smaller.");
        } catch (AuthenticationException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "A valid bearer token is required.");
        } catch (ForbiddenException e) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to perform this action.");
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Civic reporting database request failed", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The request could not be completed.");
        }
    }

    private boolean configureCors(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return true;
        }

        String configuredOrigins = System.getenv("SMARTCITY_CORS_ALLOWED_ORIGINS");
        boolean originAllowed = configuredOrigins != null
                && java.util.Arrays.stream(configuredOrigins.split(","))
                        .map(String::trim)
                        .anyMatch(origin::equals);
        if (!originAllowed) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "This website is not allowed to access the API.");
            return false;
        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Vary", "Origin");
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
            response.setHeader("Access-Control-Max-Age", "600");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }
        return true;
    }

    private void route(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        String method = request.getMethod();
        String path = request.getPathInfo();
        if (path == null) {
            path = "/";
        }

        if (path.equals("/auth/register") && method.equals("POST")) {
            register(request, response);
        } else if (path.equals("/auth/login") && method.equals("POST")) {
            login(request, response);
        } else if (path.equals("/auth/me") && method.equals("GET")) {
            sendJson(response, Map.of("success", true, "user", userView(requireUser(request))));
        } else if (path.equals("/categories") && method.equals("GET")) {
            sendJson(response, Map.of("success", true, "categories", categoryDAO.getAllCategories()));
        } else if (path.equals("/stats") && method.equals("GET")) {
            publicStats(response);
        } else if (path.equals("/issues") && method.equals("POST")) {
            submitIssue(request, response);
        } else if (path.equals("/issues") && method.equals("GET")) {
            listIssues(request, response);
        } else if (path.equals("/issues/my-reports") && method.equals("GET")) {
            User user = requireCitizen(request);
            sendJson(response, Map.of("success", true, "issues", reportDAO.getReportsByUser(user.getId())));
        } else if (path.startsWith("/track/") && method.equals("GET")) {
            trackIssue(path.substring("/track/".length()), response);
        } else if (path.startsWith("/issues/")) {
            routeIssueAction(request, response, path);
        } else if (path.equals("/admin/dashboard") && method.equals("GET")) {
            adminDashboard(request, response);
        } else if (path.equals("/admin/analytics") && method.equals("GET")) {
            analytics(request, response);
        } else if (path.equals("/admin/hotspots") && method.equals("GET")) {
            sendJson(response, Map.of("success", true, "hotspots", reportDAO.getHotspots()));
        } else if (path.startsWith("/admin/issues/")) {
            routeAdminIssueAction(request, response, path);
        } else if (path.equals("/notifications") && method.equals("GET")) {
            notifications(request, response);
        } else if (path.equals("/notifications/read-all") && method.equals("PATCH")) {
            categoryDAO.markAllAsRead(requireUser(request).getId());
            sendJson(response, Map.of("success", true));
        } else if (path.startsWith("/notifications/") && path.endsWith("/read") && method.equals("PATCH")) {
            markNotificationRead(request, response, path);
        } else if (path.equals("/leaderboard") && method.equals("GET")) {
            leaderboard(response);
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "API endpoint not found.");
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        JsonObject body = readJson(request);
        String name = requiredString(body, "name", 100);
        String email = requiredString(body, "email", 150);
        String password = requiredString(body, "password", 128);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        User user = userDAO.register(name, email, password,
                optionalString(body, "phone", 20), optionalString(body, "ward", 100));
        if (user == null) {
            sendError(response, HttpServletResponse.SC_CONFLICT, "An account with this email already exists.");
            return;
        }
        sendJson(response, Map.of("success", true, "message", "Account created successfully.",
                "token", AuthTokenUtil.issue(user), "user", userView(user)));
    }

    private void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        JsonObject body = readJson(request);
        User user = userDAO.login(requiredString(body, "email", 150), requiredString(body, "password", 128));
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Email or password is incorrect.");
            return;
        }
        sendJson(response, Map.of("success", true, "message", "Welcome back, " + user.getName() + ".",
                "token", AuthTokenUtil.issue(user), "user", userView(user)));
    }

    private void submitIssue(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException, SQLException {
        User user = requireCitizen(request);
        Part photo = getUploadPart(request, "photo");
        int categoryId = requiredInt(request.getParameter("category_id"), "category_id");
        if (categoryDAO.getAllCategories().stream().noneMatch(category -> category.getId() == categoryId)) {
            throw new IllegalArgumentException("Select a valid issue category.");
        }
        String title = requiredString(request.getParameter("title"), "title", 200);
        String description = requiredString(request.getParameter("description"), "description", 5000);
        String priority = optionalString(request.getParameter("priority"), "priority", 20);
        if (priority == null) {
            priority = "Medium";
        }
        if (!List.of("Low", "Medium", "High", "Critical").contains(priority)) {
            throw new IllegalArgumentException("Priority is not valid.");
        }
        double latitude = requiredCoordinate(request.getParameter("latitude"), "latitude", -90, 90);
        double longitude = requiredCoordinate(request.getParameter("longitude"), "longitude", -180, 180);
        StoredImage image = storeImage(photo);
        boolean saved = false;
        try {
            ReportDAO.ReportSubmissionResult submission = reportDAO.submitReport(
                    user.getId(), user.getName(), categoryId, title, description,
                    optionalString(request.getParameter("landmark"), "landmark", 200),
                    optionalString(request.getParameter("address"), "address", 255),
                    optionalString(request.getParameter("ward"), "ward", 100),
                    latitude, longitude, image.webPath(), priority);
            saved = !submission.duplicate();
            Report report = reportDAO.getReportById(submission.reportId(), user.getId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("report_id", submission.reportId());
            result.put("tracking_id", report.getTrackingId());
            result.put("issue", report);
            result.put("duplicate", submission.duplicate());
            result.put("upvoted", submission.upvoted());
            result.put("message", submission.duplicate()
                    ? submission.upvoted()
                            ? "A nearby active report already exists. Your support was added to that report."
                            : "A nearby active report already exists and was already upvoted by you."
                    : "Your civic report was submitted successfully.");
            sendJson(response, result);
        } finally {
            if (!saved) {
                Files.deleteIfExists(image.filePath());
            }
        }
    }

    private void listIssues(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        String category = request.getParameter("category_id");
        String status = request.getParameter("status");
        String ward = request.getParameter("ward");
        String search = request.getParameter("search");
        String sort = request.getParameter("sort");
        User user = optionalUser(request);
        List<Report> issues = reportDAO.getAllIssues(category, status, ward, search, sort,
                user == null ? 0 : user.getId());
        sendJson(response, Map.of("success", true, "issues", issues));
    }

    private void trackIssue(String trackingId, HttpServletResponse response)
            throws IOException, SQLException {
        if (trackingId.isBlank()) {
            throw new IllegalArgumentException("Tracking ID is required.");
        }
        Report report = reportDAO.trackByTrackingId(trackingId);
        if (report == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "No report was found for that tracking ID.");
            return;
        }
        sendJson(response, Map.of("success", true, "issue", report));
    }

    private void routeIssueAction(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, SQLException {
        String[] parts = path.substring("/issues/".length()).split("/");
        if (parts.length == 2 && parts[1].equals("upvote") && request.getMethod().equals("POST")) {
            int reportId = positiveId(parts[0]);
            User user = requireCitizen(request);
            Report report = reportDAO.getReportById(reportId);
            if (report == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Report not found.");
                return;
            }
            if (report.getUserId() == user.getId()) {
                throw new IllegalArgumentException("You cannot upvote your own report.");
            }
            if ("Resolved".equals(report.getStatus())) {
                throw new IllegalArgumentException("Resolved reports cannot receive new upvotes.");
            }
            boolean upvoted = reportDAO.toggleUpvote(reportId, user.getId());
            Report updated = reportDAO.getReportById(reportId);
            sendJson(response, Map.of("success", true, "upvoted", upvoted, "upvotes", updated.getUpvotes(),
                    "message", upvoted ? "Issue upvoted. Civic points awarded." : "Upvote removed."));
        } else if (parts.length == 2 && parts[1].equals("feedback") && request.getMethod().equals("POST")) {
            submitFeedback(request, response, positiveId(parts[0]));
        } else if (parts.length == 1 && request.getMethod().equals("GET")) {
            issueDetails(request, response, positiveId(parts[0]));
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "API endpoint not found.");
        }
    }

    private void issueDetails(HttpServletRequest request, HttpServletResponse response, int reportId)
            throws IOException, SQLException {
        User user = optionalUser(request);
        Report report = reportDAO.getReportById(reportId, user == null ? 0 : user.getId());
        if (report == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Report not found.");
            return;
        }
        sendJson(response, Map.of("success", true, "issue", report));
    }

    private void submitFeedback(HttpServletRequest request, HttpServletResponse response, int reportId)
            throws IOException, SQLException {
        User user = requireCitizen(request);
        Report report = reportDAO.getReportById(reportId);
        if (report == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Report not found.");
            return;
        }
        if (report.getUserId() != user.getId() || !"Resolved".equals(report.getStatus())) {
            throw new IllegalArgumentException("Only the reporting citizen can rate a resolved report.");
        }

        JsonObject body = readJson(request);
        int rating = requiredInt(stringOrNumber(body, "rating"), "rating");
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (!body.has("is_satisfied") || !body.get("is_satisfied").isJsonPrimitive()
                || !body.getAsJsonPrimitive("is_satisfied").isBoolean()) {
            throw new IllegalArgumentException("is_satisfied must be a boolean.");
        }
        boolean satisfied = body.get("is_satisfied").getAsBoolean();
        boolean reopened = reportDAO.submitFeedback(reportId, user.getId(), user.getName(), rating, satisfied,
                optionalString(body.has("comments") && !body.get("comments").isJsonNull()
                        ? body.get("comments").getAsString() : null, "comments", 2000));
        sendJson(response, Map.of("success", true, "reopened", reopened,
                "message", reopened ? "Report reopened for further action." : "Feedback recorded."));
    }

    private void adminDashboard(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        requireAdmin(request);
        int[] stats = reportDAO.getDashboardStats();
        Map<String, Object> dashboardStats = new LinkedHashMap<>();
        dashboardStats.put("total", stats[0]);
        dashboardStats.put("pending", stats[1]);
        dashboardStats.put("in_progress", stats[2]);
        dashboardStats.put("resolved", stats[3]);
        dashboardStats.put("critical", stats[4]);
        dashboardStats.put("high_priority", stats[7]);
        dashboardStats.put("citizens", stats[5]);
        dashboardStats.put("resolution_rate", stats[6]);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("stats", dashboardStats);
        result.put("issues", reportDAO.getAdminReports(
                request.getParameter("status"), request.getParameter("category_id"), request.getParameter("ward")));
        result.put("category_distribution", reportDAO.getCategoryAnalytics());
        result.put("status_distribution", reportDAO.getStatusAnalytics());
        sendJson(response, result);
    }

    private void publicStats(HttpServletResponse response) throws IOException, SQLException {
        int[] stats = reportDAO.getDashboardStats();
        Map<String, Object> publicStats = new LinkedHashMap<>();
        publicStats.put("total", stats[0]);
        publicStats.put("pending", stats[1]);
        publicStats.put("in_progress", stats[2]);
        publicStats.put("resolved", stats[3]);
        publicStats.put("critical", stats[4]);
        publicStats.put("citizens", stats[5]);
        sendJson(response, Map.of("success", true, "stats", publicStats));
    }

    private void analytics(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        requireAdmin(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("area_wise", reportDAO.getAreaAnalytics());
        result.put("category_wise", reportDAO.getCategoryAnalytics());
        result.put("status_wise", reportDAO.getStatusAnalytics());
        result.put("average_resolution_hours", reportDAO.getAverageResolutionTimeByCategory());
        result.put("hotspots", reportDAO.getHotspots());
        sendJson(response, result);
    }

    private void routeAdminIssueAction(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, ServletException, SQLException {
        User admin = requireAdmin(request);
        String[] parts = path.substring("/admin/issues/".length()).split("/");
        if (parts.length != 2 || !parts[1].equals("status") || !request.getMethod().equals("PATCH")) {
            if (parts.length == 2 && parts[1].equals("resolve") && request.getMethod().equals("POST")) {
                resolveIssue(request, response, admin, positiveId(parts[0]));
                return;
            }
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "API endpoint not found.");
            return;
        }

        JsonObject body = readJson(request);
        String status = requiredString(body, "status", 30);
        if (!status.equals("Pending") && !status.equals("In Progress")) {
            throw new IllegalArgumentException("Use the resolution endpoint to resolve a report.");
        }
        String priority = optionalString(body.has("priority") && !body.get("priority").isJsonNull()
                ? body.get("priority").getAsString() : null, "priority", 20);
        if (priority != null && !List.of("Low", "Medium", "High", "Critical").contains(priority)) {
            throw new IllegalArgumentException("Priority is not valid.");
        }
        reportDAO.updateStatus(positiveId(parts[0]), status,
                optionalString(body.has("remarks") && !body.get("remarks").isJsonNull()
                        ? body.get("remarks").getAsString() : null, "remarks", 2000),
                priority, admin.getName());
        sendJson(response, Map.of("success", true, "message", "Report status updated."));
    }

    private void resolveIssue(HttpServletRequest request, HttpServletResponse response, User admin, int reportId)
            throws IOException, ServletException, SQLException {
        Report report = reportDAO.getReportById(reportId);
        if (report == null) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Report not found.");
            return;
        }
        StoredImage image = storeImage(getUploadPart(request, "after_photo"));
        boolean saved = false;
        try {
            reportDAO.resolveIssue(reportId, admin.getId(), admin.getName(), image.webPath(),
                    optionalString(request.getParameter("remarks"), "remarks", 2000),
                    optionalString(request.getParameter("action_taken"), "action_taken", 200));
            saved = true;
            sendJson(response, Map.of("success", true, "message", "Report resolved with photo proof."));
        } finally {
            if (!saved) {
                Files.deleteIfExists(image.filePath());
            }
        }
    }

    private void notifications(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        User user = requireUser(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("notifications", categoryDAO.getNotifications(user.getId()));
        result.put("unread_count", categoryDAO.getUnreadCount(user.getId()));
        sendJson(response, result);
    }

    private void markNotificationRead(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, SQLException {
        String idText = path.substring("/notifications/".length(), path.length() - "/read".length());
        categoryDAO.markAsRead(positiveId(idText), requireUser(request).getId());
        sendJson(response, Map.of("success", true));
    }

    private void leaderboard(HttpServletResponse response) throws IOException, SQLException {
        List<User> users = userDAO.getLeaderboard(10);
        List<Map<String, Object>> leaderboard = new java.util.ArrayList<>();
        for (int index = 0; index < users.size(); index++) {
            User user = users.get(index);
            int rank = index + 1;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", user.getId());
            row.put("name", user.getName());
            row.put("ward", user.getWard());
            row.put("civic_points", user.getCivicPoints());
            row.put("total_reported", user.getTotalReported());
            row.put("resolved_count", user.getTotalResolved());
            row.put("rank", rank);
            row.put("badge", rank == 1 ? "Civic Champion" : rank == 2 ? "Community Guardian"
                    : rank == 3 ? "Neighborhood Leader" : "Civic Contributor");
            leaderboard.add(row);
        }
        sendJson(response, Map.of("success", true, "leaderboard", leaderboard));
    }

    private User requireUser(HttpServletRequest request) throws SQLException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationException();
        }
        int userId = AuthTokenUtil.userIdFrom(authorization.substring(7).trim());
        if (userId < 1) {
            throw new AuthenticationException();
        }
        User user = userDAO.getUserById(userId);
        if (user == null) {
            throw new AuthenticationException();
        }
        return user;
    }

    private User optionalUser(HttpServletRequest request) throws SQLException {
        String authorization = request.getHeader("Authorization");
        return authorization == null ? null : requireUser(request);
    }

    private User requireAdmin(HttpServletRequest request) throws SQLException {
        User user = requireUser(request);
        if (!user.isAdmin()) {
            throw new ForbiddenException();
        }
        return user;
    }

    private User requireCitizen(HttpServletRequest request) throws SQLException {
        User user = requireUser(request);
        if (!"citizen".equals(user.getRole())) {
            throw new ForbiddenException();
        }
        return user;
    }

    private StoredImage storeImage(Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("A photo is required.");
        }
        if (part.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new PhotoTooLargeException();
        }
        byte[] content;
        try (var input = part.getInputStream()) {
            content = input.readAllBytes();
        }
        String extension = imageExtension(content);
        if (extension == null) {
            throw new IllegalArgumentException("Photo must be a valid JPEG, PNG, or WebP image.");
        }

        String uploadDirectory = getServletContext().getRealPath("/uploads");
        if (uploadDirectory == null) {
            throw new IOException("The servlet container has no writable web application directory.");
        }
        Path directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String fileName = UUID.randomUUID() + extension;
        Path destination = directory.resolve(fileName).normalize();
        if (!destination.getParent().equals(directory)) {
            throw new IOException("Invalid upload destination.");
        }
        Files.write(destination, content);
        return new StoredImage("/uploads/" + fileName, destination);
    }

    private Part getUploadPart(HttpServletRequest request, String name) throws IOException, ServletException {
        try {
            return request.getPart(name);
        } catch (IllegalStateException e) {
            throw new PhotoTooLargeException();
        }
    }

    private static String imageExtension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G') {
            return ".png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ".webp";
        }
        return null;
    }

    private static JsonObject readJson(HttpServletRequest request) throws IOException {
        try {
            JsonObject body = JsonParser.parseReader(request.getReader()).getAsJsonObject();
            return body;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Request body must be a valid JSON object.");
        }
    }

    private static String requiredString(JsonObject object, String name, int maxLength) {
        return requiredString(object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsString() : null, name, maxLength);
    }

    private static String requiredString(String value, String name, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " is required and must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    private static String optionalString(JsonObject object, String name, int maxLength) {
        return optionalString(object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsString() : null, name, maxLength);
    }

    private static String optionalString(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    private static int requiredInt(String value, String name) {
        try {
            int number = Integer.parseInt(value);
            if (number < 1) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(name + " must be a positive integer.");
        }
    }

    private static String stringOrNumber(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return null;
        }
        return object.get(name).getAsString();
    }

    private static double requiredCoordinate(String value, String name, double min, double max) {
        try {
            double coordinate = Double.parseDouble(value);
            if (!Double.isFinite(coordinate) || coordinate < min || coordinate > max) {
                throw new NumberFormatException();
            }
            return coordinate;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(name + " must be a valid coordinate.");
        }
    }

    private static int positiveId(String value) {
        return requiredInt(value, "id");
    }

    private static Map<String, Object> userView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("name", user.getName());
        view.put("email", user.getEmail());
        view.put("phone", user.getPhone());
        view.put("role", user.getRole());
        view.put("civic_points", user.getCivicPoints());
        view.put("ward", user.getWard());
        view.put("total_reported", user.getTotalReported());
        view.put("total_resolved", user.getTotalResolved());
        return view;
    }

    private static void sendJson(HttpServletResponse response, Object body) throws IOException {
        response.getWriter().write(GSON.toJson(body));
    }

    private static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        sendJson(response, Map.of("success", false, "error", message, "message", message));
    }

    private record StoredImage(String webPath, Path filePath) {}

    private static class AuthenticationException extends SQLException {
        private static final long serialVersionUID = 1L;
    }

    private static class ForbiddenException extends SQLException {
        private static final long serialVersionUID = 1L;
    }

    private static class PhotoTooLargeException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
