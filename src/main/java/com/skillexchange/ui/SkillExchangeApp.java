package com.skillexchange.ui;

import com.skillexchange.dao.DatabaseManager;
import com.skillexchange.enums.ProficiencyLevel;
import com.skillexchange.enums.SkillCategory;
import com.skillexchange.exception.SkillExchangeException;
import com.skillexchange.model.Skill;
import com.skillexchange.model.SkillExchangeRequest;
import com.skillexchange.model.Student;
import com.skillexchange.service.SkillExchangeManager;
import com.skillexchange.thread.ConcurrentMatchDemo;
import com.skillexchange.util.FileExporter;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven console entry point. Wires together the service layer
 * (business logic + JDBC persistence), the file I/O utility, and the
 * multithreading demo - everything the syllabus covers in one place.
 */
public class SkillExchangeApp {

    private static final Scanner sc = new Scanner(System.in);
    private static SkillExchangeManager manager;

    public static void main(String[] args) {
        System.out.println("=======================================");
        System.out.println("   STUDENT SKILL EXCHANGE PLATFORM");
        System.out.println("=======================================");

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            manager = new SkillExchangeManager(db.getConnection());
            manager.loadFromDatabase();
            seedIfEmpty();
        } catch (SQLException e) {
            System.err.println("Fatal: could not initialize database - " + e.getMessage());
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> registerStudent();
                    case "2" -> addSkillToCatalog();
                    case "3" -> offerOrWantSkill();
                    case "4" -> findTeachers();
                    case "5" -> createExchangeRequest();
                    case "6" -> acceptRequest();
                    case "7" -> completeAndRate();
                    case "8" -> listStudents();
                    case "9" -> listRequests();
                    case "10" -> exportHistory();
                    case "11" -> runConcurrencyDemo();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option, try again.");
                }
            } catch (SkillExchangeException e) {
                // Custom checked exceptions from the service layer surface here.
                System.out.println("Error: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }

        try {
            DatabaseManager.getInstance().close();
        } catch (SQLException e) {
            // already have an open connection at this point in practice; ignore on shutdown
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("""

                ---- MENU ----
                1.  Register student
                2.  Add skill to catalog
                3.  Offer / want a skill (as a student)
                4.  Find teachers for a skill
                5.  Create exchange request
                6.  Accept a pending request
                7.  Complete a request & rate provider
                8.  List all students
                9.  List all requests
                10. Export request history to CSV
                11. Run concurrency demo (multithreading)
                0.  Exit
                """);
        System.out.print("Choose an option: ");
    }

    private static void registerStudent() throws SQLException {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        System.out.print("Branch (e.g. CSE): ");
        String branch = sc.nextLine().trim();
        System.out.print("Year (1-4): ");
        int year = Integer.parseInt(sc.nextLine().trim());
        Student s = manager.registerStudent(name, email, branch, year, 50); // starting credits
        System.out.println("Registered: " + s);
    }

    private static void addSkillToCatalog() throws SQLException {
        System.out.print("Skill name: ");
        String name = sc.nextLine().trim();
        System.out.println("Categories: " + java.util.Arrays.toString(SkillCategory.values()));
        System.out.print("Category: ");
        SkillCategory category = SkillCategory.valueOf(sc.nextLine().trim().toUpperCase());
        System.out.print("Description: ");
        String desc = sc.nextLine().trim();
        Skill skill = manager.addSkill(name, category, desc);
        System.out.println("Added: " + skill);
    }

    private static void offerOrWantSkill() throws SkillExchangeException, SQLException {
        int studentId = promptInt("Student id: ");
        listSkillsShort();
        int skillId = promptInt("Skill id: ");
        System.out.print("Offer to teach (T) or want to learn (L)? ");
        String kind = sc.nextLine().trim().toUpperCase();
        if (kind.equals("T")) {
            System.out.print("Proficiency [BEGINNER/INTERMEDIATE/ADVANCED/EXPERT]: ");
            ProficiencyLevel level = ProficiencyLevel.valueOf(sc.nextLine().trim().toUpperCase());
            manager.addOffer(studentId, skillId, level);
            System.out.println("Offer added.");
        } else {
            manager.addWant(studentId, skillId);
            System.out.println("Want added.");
        }
    }

    private static void findTeachers() throws SkillExchangeException {
        int studentId = promptInt("Your student id: ");
        listSkillsShort();
        int skillId = promptInt("Skill id you want to learn: ");
        List<Student> teachers = manager.findTeachersFor(skillId, studentId);
        if (teachers.isEmpty()) {
            System.out.println("No teachers found for that skill yet.");
        } else {
            System.out.println("Best matches:");
            teachers.forEach(t -> System.out.println("  " + t));
        }
    }

    private static void createExchangeRequest() throws SkillExchangeException, SQLException {
        int requesterId = promptInt("Requester (your) student id: ");
        int providerId = promptInt("Provider student id: ");
        listSkillsShort();
        int skillId = promptInt("Skill id: ");
        int credits = promptInt("Credits to offer: ");
        SkillExchangeRequest req = manager.createRequest(requesterId, providerId, skillId, credits);
        System.out.println("Created: " + req);
    }

    private static void acceptRequest() throws SQLException {
        int requestId = promptInt("Request id to accept: ");
        boolean ok = manager.acceptRequest(requestId);
        System.out.println(ok ? "Accepted." : "Could not accept (not pending or not found).");
    }

    private static void completeAndRate() throws SQLException {
        int requestId = promptInt("Request id to complete: ");
        manager.completeRequest(requestId);
        System.out.print("Rate the provider (1-5 stars), or 0 to skip: ");
        double stars = Double.parseDouble(sc.nextLine().trim());
        if (stars > 0) {
            manager.rateProvider(requestId, stars);
        }
        System.out.println("Done.");
    }

    private static void listStudents() {
        List<Student> students = manager.listStudents();
        if (students.isEmpty()) {
            System.out.println("No students yet.");
            return;
        }
        for (Student s : students) {
            System.out.println(s.displayProfile());
        }
    }

    private static void listRequests() {
        List<SkillExchangeRequest> requests = manager.listRequests();
        if (requests.isEmpty()) {
            System.out.println("No requests yet.");
            return;
        }
        requests.forEach(r -> System.out.println("  " + r));
    }

    private static void exportHistory() {
        try {
            String path = "exports/exchange_history.csv";
            FileExporter.exportRequestsToCsv(manager.listRequests(), path);
            System.out.println("Exported to " + path);
        } catch (java.io.IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private static void runConcurrencyDemo() throws InterruptedException {
        int requestId = promptInt("Pending request id to race on: ");
        int threads = promptInt("Number of concurrent threads (e.g. 5): ");
        new ConcurrentMatchDemo(manager).run(requestId, threads);
    }

    private static void listSkillsShort() {
        List<Skill> skills = manager.listSkills();
        for (Skill s : skills) {
            System.out.println("  " + s);
        }
    }

    private static int promptInt(String label) {
        System.out.print(label);
        return Integer.parseInt(sc.nextLine().trim());
    }

    /** Seeds a few skills from the starter catalog file on first run so the app isn't empty. */
    private static void seedIfEmpty() {
        try {
            if (!manager.listSkills().isEmpty()) return;
            String catalogPath = "resources/skill_catalog.txt";
            FileExporter.ensureDefaultCatalog(catalogPath);
            for (String[] row : FileExporter.readSkillCatalog(catalogPath)) {
                manager.addSkill(row[0], SkillCategory.valueOf(row[1]), row[2]);
            }
            System.out.println("Seeded starter skill catalog (" + manager.listSkills().size() + " skills).");
        } catch (Exception e) {
            System.out.println("Note: could not seed starter catalog - " + e.getMessage());
        }
    }
}
