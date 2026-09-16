package com.skillexchange.dao;

import com.skillexchange.enums.SkillCategory;
import com.skillexchange.model.Skill;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the `skills` table.
 * Demonstrates PreparedStatement usage (prevents SQL injection),
 * ResultSet mapping, and basic CRUD via JDBC.
 */
public class SkillDAO {

    private final Connection conn;

    public SkillDAO(Connection conn) {
        this.conn = conn;
    }

    public void save(Skill skill) throws SQLException {
        String sql = "INSERT OR REPLACE INTO skills (id, name, category, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, skill.getId());
            ps.setString(2, skill.getName());
            ps.setString(3, skill.getCategory().name());
            ps.setString(4, skill.getDescription());
            ps.executeUpdate();
        }
    }

    public List<Skill> findAll() throws SQLException {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT id, name, category, description FROM skills ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                skills.add(mapRow(rs));
            }
        }
        return skills;
    }

    public Skill findById(int id) throws SQLException {
        String sql = "SELECT id, name, category, description FROM skills WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Skill> findByCategory(SkillCategory category) throws SQLException {
        List<Skill> skills = new ArrayList<>();
        String sql = "SELECT id, name, category, description FROM skills WHERE category = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    skills.add(mapRow(rs));
                }
            }
        }
        return skills;
    }

    private Skill mapRow(ResultSet rs) throws SQLException {
        return new Skill(
                rs.getInt("id"),
                rs.getString("name"),
                SkillCategory.valueOf(rs.getString("category")),
                rs.getString("description")
        );
    }
}
