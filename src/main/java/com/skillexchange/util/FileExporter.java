package com.skillexchange.util;

import com.skillexchange.enums.SkillCategory;
import com.skillexchange.model.SkillExchangeRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Java I/O Streams portion of the syllabus: character-oriented
 * streams (BufferedWriter/BufferedReader over FileWriter/FileReader)
 * used to export exchange history to CSV and to bulk-import a
 * starter skill catalog from a plain text file.
 */
public final class FileExporter {

    private FileExporter() {
    }

    /** Writes every exchange request to a CSV file using a character stream (BufferedWriter). */
    public static void exportRequestsToCsv(List<SkillExchangeRequest> requests, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            writer.write("RequestId,Requester,Provider,Skill,Credits,Status,CreatedAt");
            writer.newLine();
            for (SkillExchangeRequest r : requests) {
                writer.write(String.format("%d,%s,%s,%s,%d,%s,%s",
                        r.getId(),
                        r.getRequester().getName(),
                        r.getProvider().getName(),
                        r.getSkill().getName(),
                        r.getCreditsOffered(),
                        r.getStatus(),
                        r.getCreatedAt()));
                writer.newLine();
            }
        }
        // IOException is intentionally allowed to propagate (throws clause) so
        // the caller decides how to handle it - matches the syllabus's
        // throw/throws + try-catch topic when demoed from the console layer.
    }

    /**
     * Reads a starter skill catalog from a plain text file, one entry
     * per line in the form: name|CATEGORY|description
     * Uses a character stream (BufferedReader over FileReader).
     * Malformed lines are skipped with a warning rather than aborting
     * the whole import (defensive I/O handling).
     */
    public static List<String[]> readSkillCatalog(String filePath) throws IOException {
        List<String[]> rows = new java.util.ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length != 3) {
                    System.err.println("Skipping malformed catalog line " + lineNo + ": " + line);
                    continue;
                }
                try {
                    SkillCategory.valueOf(parts[1].trim()); // validate category name
                    rows.add(new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping line " + lineNo + " - unknown category: " + parts[1]);
                }
            }
        }
        return rows;
    }

    /** Creates a default starter catalog file if one doesn't already exist yet. */
    public static void ensureDefaultCatalog(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("# name|CATEGORY|description\n");
            writer.write("Python Programming|TECHNOLOGY|Basics to intermediate Python\n");
            writer.write("Guitar Basics|MUSIC|Chords, strumming, and basic songs\n");
            writer.write("Public Speaking|LIFE_SKILLS|Confidence and structuring a talk\n");
            writer.write("Sketching|ART_AND_DESIGN|Pencil sketching fundamentals\n");
            writer.write("Spanish Conversation|LANGUAGE|Everyday conversational Spanish\n");
            writer.write("Data Structures|ACADEMICS|DSA concepts for interviews\n");
            writer.write("Badminton|SPORTS|Footwork, smashes, and match strategy\n");
        }
    }
}
