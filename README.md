# Student Skill Exchange Platform

A console-based Java application where students list skills they can
teach and skills they want to learn, get matched with peers, and
trade sessions using a simple credit system.

Built as a Java course project — the design deliberately touches
every topic on a typical Java syllabus (see mapping below).

## Requirements
- JDK 17 or later (uses `switch` expressions, text blocks, `var`)
- No external server needed — persistence uses SQLite, a file-based
  database, via the bundled JDBC driver in `lib/sqlite-jdbc.jar`
  (org.xerial sqlite-jdbc 3.45.3.0, bundles native libraries for
  Windows, macOS, and Linux — nothing extra to install)



## How to run

**macOS / Linux:**
```bash
javac -cp "lib/*" -d out $(find src/main/java -name "*.java")
java -cp "out:lib/*" com.skillexchange.ui.SkillExchangeApp
```

**Windows (PowerShell):**
```powershell
javac -cp "lib/*" -d out (Get-ChildItem -Recurse -Filter *.java -Path src\main\java | ForEach-Object { $_.FullName })
java -cp "out;lib/*" com.skillexchange.ui.SkillExchangeApp
```

Note the classpath separator differs: `:` on macOS/Linux, `;` on
Windows. Also note PowerShell doesn't have a Unix-style `find`, so
compiling needs the `Get-ChildItem` form above instead of
`$(find ...)`.

## Web frontend

**macOS / Linux:**
```bash
javac -cp "lib/*" -d out $(find src/main/java -name "*.java")
java -cp "out:lib/*" com.skillexchange.web.WebApp 8080
```

**Windows (PowerShell):**
```powershell
javac -cp "lib/*" -d out (Get-ChildItem -Recurse -Filter *.java -Path src\main\java | ForEach-Object { $_.FullName })
java -cp "out;lib/*" com.skillexchange.web.WebApp 8080
```

REST API summary (all JSON responses, POST bodies as
`application/x-www-form-urlencoded`):

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/students` | list / register students |
| GET/POST | `/api/skills` | list / add catalog skills |
| POST | `/api/offer`, `/api/want` | link a student to a skill |
| GET | `/api/teachers?skillId=&requesterId=` | ranked matches |
| GET/POST | `/api/requests` | list / create exchange requests |
| POST | `/api/accept`, `/api/complete`, `/api/rate` | request lifecycle |
| GET | `/api/export` | write CSV to `exports/` |
| POST | `/api/concurrency-demo` | run the threading race, return log |

## Project layout

```
src/main/java/com/skillexchange/
  model/       User (abstract), Student, Skill, SkillOffer,
               SkillExchangeRequest, Rateable & Matchable interfaces
  enums/       SkillCategory, ProficiencyLevel, ExchangeStatus
  exception/   SkillExchangeException (base) and 4 specific
               checked subclasses
  service/     SkillExchangeManager (facade + in-memory collections),
               MatchEngine (singleton matching logic)
  dao/         DatabaseManager (singleton JDBC connection + schema),
               StudentDAO, SkillDAO, ExchangeRequestDAO
  thread/      ConcurrentMatchDemo (multithreading demo)
  util/        FileExporter (character-stream I/O: CSV export,
               catalog import)
  ui/          SkillExchangeApp (console menu, the entry point)
  web/         ApiServer (REST API + static file serving), WebApp (entry point)

web/           Frontend: index.html, style.css, app.js
```

## How this maps to the syllabus

| Syllabus topic | Where it lives |
|---|---|
| Classes, objects, constructors, `this`, access modifiers | `model/*.java` |
| Inheritance & polymorphism | `User` (abstract) → `Student`; `displayProfile()`/`getRole()` overridden |
| Interfaces | `Rateable`, `Matchable` |
| Enums (with constructors/fields) | `SkillCategory`, `ProficiencyLevel` |
| Singleton pattern | `MatchEngine`, `DatabaseManager` |
| Exception handling, custom exceptions, throw/throws | `exception/*`, used throughout `service/SkillExchangeManager.java` |
| Collections Framework (List, Map, ArrayList, HashMap) | `SkillExchangeManager` (`ConcurrentHashMap`, `ArrayList`) |
| Multithreading & synchronization | `acceptRequest()` synchronized block + `thread/ConcurrentMatchDemo.java` |
| I/O Streams (character streams) | `util/FileExporter.java` (BufferedReader/BufferedWriter) |
| JDBC | `dao/*.java` — `PreparedStatement`, `ResultSet`, CRUD, driver loading |
| Strings, arrays, Scanner input | `ui/SkillExchangeApp.java` |

## Extending to JPA

The `dao/` package is hand-written JDBC by design so every line maps
directly to what the syllabus teaches. To extend this into the JPA
portion of the course: turn `Student`/`Skill`/`SkillExchangeRequest`
into `@Entity` classes, add `@OneToMany`/`@ManyToOne` mappings for
the offer/want/request relationships, and replace the DAO classes
with a JPA `EntityManager` — the service layer (`SkillExchangeManager`)
would barely need to change since it only calls DAO methods, not raw
SQL.
## Demo script for evaluation

1. Register 2–3 students (menu 1)
2. Have one offer a skill, another want it (menu 3)
3. Find teachers for that skill (menu 4) — shows matching + polymorphism
4. Create a request (menu 5), accept it (menu 6), complete & rate (menu 7)
5. Export history to CSV (menu 10) — shows file I/O
6. Run the concurrency demo (menu 11) on a *new* pending request — shows
   several threads racing to accept it, only one winning (synchronization)

## Troubleshooting

**`NativeLibraryNotFoundException` / "Error opening connection" on startup**
The bundled `lib/sqlite-jdbc.jar` must include native binaries for
your OS. If you get this error, your jar is likely an incomplete
build (e.g. one that only bundles macOS binaries). Re-download the
official jar from
`https://github.com/xerial/sqlite-jdbc/releases/download/3.45.3.0/sqlite-jdbc-3.45.3.0.jar`
(~13.5 MB — if yours is closer to 1–2 MB, it's missing platform
binaries) and replace `lib/sqlite-jdbc.jar` with it.

**`error: no source files` after `javac`**
This means the file-finding part of the compile command returned
nothing. On Windows PowerShell, `$(find ...)` doesn't work — use the
`Get-ChildItem` form shown above instead. Also double check you're
running the command from the project **root** folder (the one
containing `src`, `lib`, and `web`), not from inside `web/` or a
nested duplicate folder.

**`Could not find or load main class`**
Usually a typo in the fully-qualified class name (it's
`com.skillexchange.web.WebApp`, not `com.skilllexchange...` — one
`l` in "skill") or the `out` folder is empty because the compile
step above failed. Check `out/com/skillexchange/web/WebApp.class`
exists before re-running.

**SLF4J warning ("Failed to load class StaticLoggerBinder")**
Harmless — it's just SQLite's logging library falling back to a
no-op logger since none is configured. Doesn't affect functionality.