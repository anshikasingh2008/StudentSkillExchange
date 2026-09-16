package com.skillexchange.dao;

import com.skillexchange.enums.ProficiencyLevel;
import com.skillexchange.model.Skill;
import com.skillexchange.model.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for students, including their skill offers and
 * wants (a simple one-to-many mapping done by hand with JDBC - the
 * JPA/ORM equivalent of this class is described in README.md as an
 * extension point for the "Persistence API" portion of the syllabus).
 */
public class StudentDAO {

    private final Connection conn;
    private final SkillDAO skillDAO;

    public StudentDAO(Connection conn) {
        this.conn = conn;
        this.skillDAO = new SkillDAO(conn);
    }

    public void save(Student student) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO users (id, name, email, branch, year, credits, rating_total, rating_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, student.getId());
            ps.setString(2, student.getName());
            ps.setString(3, student.getEmail());
            ps.setString(4, student.getBranch());
            ps.setInt(5, student.getYear());
            ps.setInt(6, student.getCredits());
            ps.setDouble(7, student.getAverageRating() * student.getRatingCount());
            ps.setInt(8, student.getRatingCount());
            ps.executeUpdate();
        }
        saveOffers(student);
        saveWants(student);
    }

    private void saveOffers(Student student) throws SQLException {
        String delete = "DELETE FROM skill_offers WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(delete)) {
            ps.setInt(1, student.getId());
            ps.executeUpdate();
        }
        String insert = "INSERT INTO skill_offers (user_id, skill_id, level) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            for (var offer : student.getSkillsOffered()) {
                ps.setInt(1, student.getId());
                ps.setInt(2, offer.getSkill().getId());
                ps.setString(3, offer.getLevel().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveWants(Student student) throws SQLException {
        String delete = "DELETE FROM skill_wants WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(delete)) {
            ps.setInt(1, student.getId());
            ps.executeUpdate();
        }
        String insert = "INSERT INTO skill_wants (user_id, skill_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            for (Skill skill : student.getSkillsWanted()) {
                ps.setInt(1, student.getId());
                ps.setInt(2, skill.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, email, branch, year, credits, rating_total, rating_count FROM users ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                students.add(mapRow(rs));
            }
        }
        for (Student s : students) {
            attachOffersAndWants(s);
        }
        return students;
    }

    public Student findById(int id) throws SQLException {
        String sql = "SELECT id, name, email, branch, year, credits, rating_total, rating_count FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = mapRow(rs);
                    attachOffersAndWants(s);
                    return s;
                }
            }
        }
        return null;
    }

    private void attachOffersAndWants(Student s) throws SQLException {
        String offerSql = "SELECT skill_id, level FROM skill_offers WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(offerSql)) {
            ps.setInt(1, s.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Skill skill = skillDAO.findById(rs.getInt("skill_id"));
                    if (skill != null) {
                        s.offerSkill(skill, ProficiencyLevel.valueOf(rs.getString("level")));
                    }
                }
            }
        }
        String wantSql = "SELECT skill_id FROM skill_wants WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(wantSql)) {
            ps.setInt(1, s.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Skill skill = skillDAO.findById(rs.getInt("skill_id"));
                    if (skill != null) {
                        s.wantSkill(skill);
                    }
                }
            }
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Student s = new Student(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getInt("credits"),
                rs.getString("branch"),
                rs.getInt("year")
        );
        s.restoreRatingStats(rs.getDouble("rating_total"), rs.getInt("rating_count"));
        return s;
    }
}
