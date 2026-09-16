package com.skillexchange.service;

import com.skillexchange.dao.ExchangeRequestDAO;
import com.skillexchange.dao.SkillDAO;
import com.skillexchange.dao.StudentDAO;
import com.skillexchange.enums.ExchangeStatus;
import com.skillexchange.enums.ProficiencyLevel;
import com.skillexchange.enums.SkillCategory;
import com.skillexchange.exception.*;
import com.skillexchange.model.Skill;
import com.skillexchange.model.SkillExchangeRequest;
import com.skillexchange.model.Student;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The central service (facade) that the UI layer talks to. Owns the
 * in-memory collections (Java Collections Framework: HashMap, ArrayList)
 * that back the app during a session, and persists changes via the
 * DAO layer (JDBC). Also the place where custom exceptions are thrown
 * and where thread-safety for concurrent requests is enforced.
 */
public class SkillExchangeManager {

    // Collections Framework in action: fast lookup by id via HashMap,
    // ordered listing via ArrayList-backed values().
    private final Map<Integer, Student> students = new ConcurrentHashMap<>();
    private final Map<Integer, Skill> skills = new ConcurrentHashMap<>();
    private final Map<Integer, SkillExchangeRequest> requests = new ConcurrentHashMap<>();

    private final AtomicInteger studentIdSeq = new AtomicInteger(0);
    private final AtomicInteger skillIdSeq = new AtomicInteger(0);
    private final AtomicInteger requestIdSeq = new AtomicInteger(0);

    private final StudentDAO studentDAO;
    private final SkillDAO skillDAO;
    private final ExchangeRequestDAO requestDAO;

    // Guards the "accept a match" critical section so two threads can
    // never both accept the same pending request (see thread package).
    private final Object matchLock = new Object();

    public SkillExchangeManager(Connection connection) {
        this.studentDAO = new StudentDAO(connection);
        this.skillDAO = new SkillDAO(connection);
        this.requestDAO = new ExchangeRequestDAO(connection);
    }

    // ---------- Skill catalog ----------

    public Skill addSkill(String name, SkillCategory category, String description) throws SQLException {
        int id = skillIdSeq.incrementAndGet();
        Skill skill = new Skill(id, name, category, description);
        skills.put(id, skill);
        skillDAO.save(skill);
        return skill;
    }

    public List<Skill> listSkills() {
        return new ArrayList<>(skills.values());
    }

    public Skill getSkill(int id) throws SkillNotFoundException {
        Skill skill = skills.get(id);
        if (skill == null) {
            throw new SkillNotFoundException("No skill found with id " + id);
        }
        return skill;
    }

    // ---------- Students ----------

    public Student registerStudent(String name, String email, String branch, int year, int startingCredits)
            throws SQLException {
        int id = studentIdSeq.incrementAndGet();
        Student student = new Student(id, name, email, startingCredits, branch, year);
        students.put(id, student);
        studentDAO.save(student);
        return student;
    }

    public List<Student> listStudents() {
        return new ArrayList<>(students.values());
    }

    public Student getStudent(int id) throws UserNotFoundException {
        Student student = students.get(id);
        if (student == null) {
            throw new UserNotFoundException("No student found with id " + id);
        }
        return student;
    }

    public void addOffer(int studentId, int skillId, ProficiencyLevel level)
            throws UserNotFoundException, SkillNotFoundException, SQLException {
        Student student = getStudent(studentId);
        Skill skill = getSkill(skillId);
        student.offerSkill(skill, level);
        studentDAO.save(student);
    }

    public void addWant(int studentId, int skillId)
            throws UserNotFoundException, SkillNotFoundException, SQLException {
        Student student = getStudent(studentId);
        Skill skill = getSkill(skillId);
        student.wantSkill(skill);
        studentDAO.save(student);
    }

    // ---------- Matching ----------

    public List<Student> findTeachersFor(int skillId, int requesterId)
            throws SkillNotFoundException, UserNotFoundException {
        Skill skill = getSkill(skillId);
        Student requester = getStudent(requesterId);
        return MatchEngine.getInstance().findTeachersFor(skill, requester, listStudents());
    }

    // ---------- Exchange requests ----------

    public SkillExchangeRequest createRequest(int requesterId, int providerId, int skillId, int creditsOffered)
            throws UserNotFoundException, SkillNotFoundException, DuplicateRequestException,
                   InsufficientCreditsException, SQLException {

        Student requester = getStudent(requesterId);
        Student provider = getStudent(providerId);
        Skill skill = getSkill(skillId);

        if (requester.getCredits() < creditsOffered) {
            throw new InsufficientCreditsException(
                    requester.getName() + " has only " + requester.getCredits() +
                    " credits but offered " + creditsOffered);
        }

        boolean duplicate = requests.values().stream().anyMatch(r ->
                r.getRequester().equals(requester) &&
                r.getProvider().equals(provider) &&
                r.getSkill().equals(skill) &&
                r.getStatus() == ExchangeStatus.PENDING);
        if (duplicate) {
            throw new DuplicateRequestException(
                    requester.getName() + " already has a pending request to " +
                    provider.getName() + " for " + skill.getName());
        }

        int id = requestIdSeq.incrementAndGet();
        SkillExchangeRequest request = new SkillExchangeRequest(id, requester, provider, skill, creditsOffered);
        requests.put(id, request);
        requestDAO.save(request);
        return request;
    }

    /**
     * Accepts a pending request: transfers credits and marks it
     * accepted. Synchronized on a shared lock so that if two threads
     * (e.g. two simultaneous "accept" clicks in a real app) race on
     * the same request, only one can succeed - the other sees it is
     * no longer PENDING and backs off. This is the multithreading +
     * synchronization requirement from the syllabus, demoed live in
     * ConcurrentMatchDemo.
     */
    public boolean acceptRequest(int requestId) throws SQLException {
        synchronized (matchLock) {
            SkillExchangeRequest request = requests.get(requestId);
            if (request == null || request.getStatus() != ExchangeStatus.PENDING) {
                return false; // already handled by another thread, or doesn't exist
            }
            request.setStatus(ExchangeStatus.ACCEPTED);
            requestDAO.save(request);
            return true;
        }
    }

    public void completeRequest(int requestId) throws SQLException {
        synchronized (matchLock) {
            SkillExchangeRequest request = requests.get(requestId);
            if (request == null || request.getStatus() != ExchangeStatus.ACCEPTED) {
                return;
            }
            Student requester = request.getRequester();
            Student provider = request.getProvider();
            requester.deductCredits(request.getCreditsOffered());
            provider.addCredits(request.getCreditsOffered());
            request.setStatus(ExchangeStatus.COMPLETED);
            requestDAO.save(request);
            studentDAO.save(requester);
            studentDAO.save(provider);
        }
    }

    public void rateProvider(int requestId, double stars) throws SQLException {
        SkillExchangeRequest request = requests.get(requestId);
        if (request != null && request.getStatus() == ExchangeStatus.COMPLETED) {
            request.getProvider().addRating(stars);
            studentDAO.save(request.getProvider());
        }
    }

    public List<SkillExchangeRequest> listRequests() {
        return new ArrayList<>(requests.values());
    }

    public List<SkillExchangeRequest> listRequestsForStudent(int studentId) {
        return requests.values().stream()
                .filter(r -> r.getRequester().getId() == studentId || r.getProvider().getId() == studentId)
                .toList();
    }

    /** Reload every student, skill, and request from the database (used at startup). */
    public void loadFromDatabase() throws SQLException {
        for (Skill s : skillDAO.findAll()) {
            skills.put(s.getId(), s);
            skillIdSeq.updateAndGet(v -> Math.max(v, s.getId()));
        }
        for (Student s : studentDAO.findAll()) {
            students.put(s.getId(), s);
            studentIdSeq.updateAndGet(v -> Math.max(v, s.getId()));
        }
        for (SkillExchangeRequest r : requestDAO.findAll()) {
            requests.put(r.getId(), r);
            requestIdSeq.updateAndGet(v -> Math.max(v, r.getId()));
        }
    }
}
