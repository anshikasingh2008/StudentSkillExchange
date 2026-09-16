package com.skillexchange.web;

import com.skillexchange.enums.ProficiencyLevel;
import com.skillexchange.enums.SkillCategory;
import com.skillexchange.exception.SkillExchangeException;
import com.skillexchange.model.Skill;
import com.skillexchange.model.SkillExchangeRequest;
import com.skillexchange.model.SkillOffer;
import com.skillexchange.model.Student;
import com.skillexchange.service.SkillExchangeManager;
import com.skillexchange.thread.ConcurrentMatchDemo;
import com.skillexchange.util.FileExporter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Thin REST wrapper around SkillExchangeManager, using only the JDK's
 * built-in com.sun.net.httpserver (no Spring / external web
 * framework needed) - the whole project still builds with a single
 * `javac` call. Serves the static frontend (web/) at "/" and JSON
 * endpoints under "/api/...".
 */
public class ApiServer {

    private final SkillExchangeManager manager;
    private final File webRoot;

    public ApiServer(SkillExchangeManager manager, File webRoot) {
        this.manager = manager;
        this.webRoot = webRoot;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/students", this::handleStudents);
        server.createContext("/api/skills", this::handleSkills);
        server.createContext("/api/requests", this::handleRequests);
        server.createContext("/api/offer", this::handleOffer);
        server.createContext("/api/want", this::handleWant);
        server.createContext("/api/teachers", this::handleTeachers);
        server.createContext("/api/accept", this::handleAccept);
        server.createContext("/api/complete", this::handleComplete);
        server.createContext("/api/rate", this::handleRate);
        server.createContext("/api/export", this::handleExport);
        server.createContext("/api/concurrency-demo", this::handleConcurrencyDemo);
        server.createContext("/", this::handleStatic);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Web server running at http://localhost:" + port + "/");
    }

    // ---------- /api/students ----------

    private void handleStudents(HttpExchange ex) throws IOException {
        try {
            if ("GET".equals(ex.getRequestMethod())) {
                List<Map<String, Object>> out = manager.listStudents().stream().map(this::studentJson).toList();
                sendJson(ex, 200, JsonUtil.toJson(out));
            } else if ("POST".equals(ex.getRequestMethod())) {
                Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
                Student s = manager.registerStudent(
                        f.get("name"), f.get("email"), f.get("branch"),
                        Integer.parseInt(f.get("year")), 50);
                sendJson(ex, 201, JsonUtil.toJson(studentJson(s)));
            } else {
                sendJson(ex, 405, err("Method not allowed"));
            }
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- /api/skills ----------

    private void handleSkills(HttpExchange ex) throws IOException {
        try {
            if ("GET".equals(ex.getRequestMethod())) {
                List<Map<String, Object>> out = manager.listSkills().stream().map(this::skillJson).toList();
                sendJson(ex, 200, JsonUtil.toJson(out));
            } else if ("POST".equals(ex.getRequestMethod())) {
                Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
                Skill s = manager.addSkill(f.get("name"),
                        SkillCategory.valueOf(f.get("category").toUpperCase()), f.get("description"));
                sendJson(ex, 201, JsonUtil.toJson(skillJson(s)));
            } else {
                sendJson(ex, 405, err("Method not allowed"));
            }
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- /api/requests ----------

    private void handleRequests(HttpExchange ex) throws IOException {
        try {
            if ("GET".equals(ex.getRequestMethod())) {
                List<Map<String, Object>> out = manager.listRequests().stream().map(this::requestJson).toList();
                sendJson(ex, 200, JsonUtil.toJson(out));
            } else if ("POST".equals(ex.getRequestMethod())) {
                Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
                SkillExchangeRequest r = manager.createRequest(
                        Integer.parseInt(f.get("requesterId")),
                        Integer.parseInt(f.get("providerId")),
                        Integer.parseInt(f.get("skillId")),
                        Integer.parseInt(f.get("credits")));
                sendJson(ex, 201, JsonUtil.toJson(requestJson(r)));
            } else {
                sendJson(ex, 405, err("Method not allowed"));
            }
        } catch (SkillExchangeException e) {
            sendJson(ex, 422, err(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- /api/offer, /api/want ----------

    private void handleOffer(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
            manager.addOffer(Integer.parseInt(f.get("studentId")), Integer.parseInt(f.get("skillId")),
                    ProficiencyLevel.valueOf(f.get("level").toUpperCase()));
            sendJson(ex, 200, JsonUtil.toJson(Map.of("ok", true)));
        } catch (SkillExchangeException e) {
            sendJson(ex, 422, err(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    private void handleWant(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
            manager.addWant(Integer.parseInt(f.get("studentId")), Integer.parseInt(f.get("skillId")));
            sendJson(ex, 200, JsonUtil.toJson(Map.of("ok", true)));
        } catch (SkillExchangeException e) {
            sendJson(ex, 422, err(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- /api/teachers?skillId=&requesterId= ----------

    private void handleTeachers(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = JsonUtil.parseForm(ex.getRequestURI().getQuery());
            List<Student> teachers = manager.findTeachersFor(
                    Integer.parseInt(q.get("skillId")), Integer.parseInt(q.get("requesterId")));
            sendJson(ex, 200, JsonUtil.toJson(teachers.stream().map(this::studentJson).toList()));
        } catch (SkillExchangeException e) {
            sendJson(ex, 422, err(e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- /api/accept, /api/complete, /api/rate ----------

    private void handleAccept(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
            boolean ok = manager.acceptRequest(Integer.parseInt(f.get("requestId")));
            sendJson(ex, 200, JsonUtil.toJson(Map.of("accepted", ok)));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    private void handleComplete(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
            manager.completeRequest(Integer.parseInt(f.get("requestId")));
            sendJson(ex, 200, JsonUtil.toJson(Map.of("ok", true)));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    private void handleRate(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
            manager.rateProvider(Integer.parseInt(f.get("requestId")), Double.parseDouble(f.get("stars")));
            sendJson(ex, 200, JsonUtil.toJson(Map.of("ok", true)));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- /api/export ----------

    private void handleExport(HttpExchange ex) throws IOException {
        try {
            String path = "exports/exchange_history.csv";
            FileExporter.exportRequestsToCsv(manager.listRequests(), path);
            sendJson(ex, 200, JsonUtil.toJson(Map.of("path", path)));
        } catch (Exception e) {
            sendJson(ex, 500, err(e.getMessage()));
        }
    }

    // ---------- /api/concurrency-demo ----------

    private void handleConcurrencyDemo(HttpExchange ex) throws IOException {
        try {
            Map<String, String> f = JsonUtil.parseForm(JsonUtil.readBody(ex.getRequestBody()));
            int requestId = Integer.parseInt(f.get("requestId"));
            int threads = Integer.parseInt(f.get("threads"));
            List<String> log = new ConcurrentMatchDemo(manager).runForApi(requestId, threads);
            sendJson(ex, 200, JsonUtil.toJson(Map.of("log", log)));
        } catch (Exception e) {
            sendJson(ex, 400, err(e.getMessage()));
        }
    }

    // ---------- static file serving ----------

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        File file = new File(webRoot, path).getCanonicalFile();
        if (!file.getPath().startsWith(webRoot.getCanonicalPath()) || !file.isFile()) {
            byte[] body = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
            return;
        }
        ex.getResponseHeaders().set("Content-Type", contentType(file.getName()));
        byte[] body = Files.readAllBytes(file.toPath());
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private String contentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    // ---------- json helpers ----------

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private String err(String message) {
        return JsonUtil.toJson(Map.of("error", message == null ? "Unknown error" : message));
    }

    private Map<String, Object> studentJson(Student s) {
        Map<String, Object> m = JsonUtil.obj();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("email", s.getEmail());
        m.put("branch", s.getBranch());
        m.put("year", s.getYear());
        m.put("credits", s.getCredits());
        m.put("rating", s.getAverageRating());
        m.put("ratingCount", s.getRatingCount());
        m.put("offers", s.getSkillsOffered().stream().map(SkillOffer::toString).toList());
        m.put("wants", s.getSkillsWanted().stream().map(Skill::getName).toList());
        return m;
    }

    private Map<String, Object> skillJson(Skill s) {
        Map<String, Object> m = JsonUtil.obj();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("category", s.getCategory().name());
        m.put("categoryLabel", s.getCategory().getDisplayName());
        m.put("description", s.getDescription());
        return m;
    }

    private Map<String, Object> requestJson(SkillExchangeRequest r) {
        Map<String, Object> m = JsonUtil.obj();
        m.put("id", r.getId());
        m.put("requesterId", r.getRequester().getId());
        m.put("requesterName", r.getRequester().getName());
        m.put("providerId", r.getProvider().getId());
        m.put("providerName", r.getProvider().getName());
        m.put("skillId", r.getSkill().getId());
        m.put("skillName", r.getSkill().getName());
        m.put("credits", r.getCreditsOffered());
        m.put("status", r.getStatus().name());
        m.put("createdAt", r.getCreatedAt().toString());
        return m;
    }
}
