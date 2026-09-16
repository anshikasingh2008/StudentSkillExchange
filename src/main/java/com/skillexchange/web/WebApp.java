package com.skillexchange.web;

import com.skillexchange.dao.DatabaseManager;
import com.skillexchange.enums.SkillCategory;
import com.skillexchange.service.SkillExchangeManager;
import com.skillexchange.util.FileExporter;

import java.io.File;
import java.sql.SQLException;

/**
 * Entry point for the web version of the app: boots the same
 * database + service layer used by the console UI, then starts the
 * embedded HTTP server so the frontend in web/ can talk to it.
 *
 * Run alongside or instead of com.skillexchange.ui.SkillExchangeApp -
 * both share the same SQLite database, so data created in one is
 * visible in the other.
 */
public class WebApp {

    public static void main(String[] args) throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        SkillExchangeManager manager = new SkillExchangeManager(db.getConnection());
        manager.loadFromDatabase();
        seedIfEmpty(manager);

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ApiServer server = new ApiServer(manager, new File("web"));
        server.start(port);
    }

    private static void seedIfEmpty(SkillExchangeManager manager) {
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
