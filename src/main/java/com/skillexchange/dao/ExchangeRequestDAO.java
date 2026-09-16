package com.skillexchange.dao;

import com.skillexchange.enums.ExchangeStatus;
import com.skillexchange.model.Skill;
import com.skillexchange.model.SkillExchangeRequest;
import com.skillexchange.model.Student;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object for the `exchange_requests` table. */
public class ExchangeRequestDAO {

    private final Connection conn;
    private final StudentDAO studentDAO;
    private final SkillDAO skillDAO;

    public ExchangeRequestDAO(Connection conn) {
        this.conn = conn;
        this.studentDAO = new StudentDAO(conn);
        this.skillDAO = new SkillDAO(conn);
    }

    public void save(SkillExchangeRequest request) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO exchange_requests
                (id, requester_id, provider_id, skill_id, credits_offered, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.getId());
            ps.setInt(2, request.getRequester().getId());
            ps.setInt(3, request.getProvider().getId());
            ps.setInt(4, request.getSkill().getId());
            ps.setInt(5, request.getCreditsOffered());
            ps.setString(6, request.getStatus().name());
            ps.setString(7, request.getCreatedAt().toString());
            ps.executeUpdate();
        }
    }

    public List<SkillExchangeRequest> findAll() throws SQLException {
        List<SkillExchangeRequest> requests = new ArrayList<>();
        String sql = """
            SELECT id, requester_id, provider_id, skill_id, credits_offered, status, created_at
            FROM exchange_requests ORDER BY id
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                requests.add(mapRow(rs));
            }
        }
        return requests;
    }

    private SkillExchangeRequest mapRow(ResultSet rs) throws SQLException {
        Student requester = studentDAO.findById(rs.getInt("requester_id"));
        Student provider = studentDAO.findById(rs.getInt("provider_id"));
        Skill skill = skillDAO.findById(rs.getInt("skill_id"));
        SkillExchangeRequest req = new SkillExchangeRequest(
                rs.getInt("id"), requester, provider, skill, rs.getInt("credits_offered")
        );
        req.setStatus(ExchangeStatus.valueOf(rs.getString("status")));
        return req;
    }
}
