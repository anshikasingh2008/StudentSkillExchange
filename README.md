# Student Skill Exchange Platform

A Java application where students can list skills they can teach, list skills they want to learn, find suitable peers, and manage skill-exchange requests using a simple credit system.

The primary interface is a command-line console menu. A browser-based web UI is also included as an optional extension; it uses the same Java backend and SQLite database.

## Requirements

- JDK 17 or later. The project uses switch expressions and text blocks.
- A terminal: PowerShell on Windows, or bash/zsh on macOS/Linux.
- No IDE, database server, or separate dependency installation is required. The SQLite JDBC driver is included in `lib/`.

Verify Java is installed:

```bash
java -version
javac -version
```

Both commands should show version 17 or later.

## Run the console application

Run these commands from the project root, the folder containing `src/`, `lib/`, and `web/`.

### Windows PowerShell

```powershell
# Compile
javac -cp "lib/*" -d out (Get-ChildItem -Recurse -Filter *.java -Path src\main\java | ForEach-Object { $_.FullName })

# Start the console application
java -cp "out;lib/*" com.skillexchange.ui.SkillExchangeApp
```

### macOS/Linux

```bash
# Compile
javac -cp "lib/*" -d out $(find src/main/java -name "*.java")

# Start the console application
java -cp "out:lib/*" com.skillexchange.ui.SkillExchangeApp
```

Windows uses `;` to separate classpath entries; macOS/Linux use `:`.

When the application starts, the terminal displays this menu:

```text
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
```

### First run

On the first launch, the application automatically creates `data/skill_exchange.db` and a starter skill catalog at `resources/skill_catalog.txt`. No manual configuration is needed.

## Optional web UI

The web UI is an additional feature, not the required course interface. It runs from the terminal and is served by the Java application itself.

### Windows PowerShell

```powershell
javac -cp "lib/*" -d out (Get-ChildItem -Recurse -Filter *.java -Path src\main\java | ForEach-Object { $_.FullName })
java -cp "out;lib/*" com.skillexchange.web.WebApp 8080
```

### macOS/Linux

```bash
javac -cp "lib/*" -d out $(find src/main/java -name "*.java")
java -cp "out:lib/*" com.skillexchange.web.WebApp 8080
```

Open `http://localhost:8080` in a browser. Press `Ctrl+C` in the terminal to stop the server.

## Features

- Student registration with name, email, branch, year, and credits
- Skill catalog with categories and descriptions
- Teaching offers and learning requests
- Teacher matching based on offered skills
- Exchange-request lifecycle: create, accept, complete, and rate
- Credit transfer after a completed exchange
- SQLite persistence through JDBC
- CSV export of exchange history to `exports/`
- Thread-safe request acceptance, demonstrated with a multithreading race
- Optional REST API and browser frontend

## REST API summary

All API responses are JSON. POST requests use `application/x-www-form-urlencoded` bodies.

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/api/students` | List or register students |
| GET/POST | `/api/skills` | List or add catalog skills |
| POST | `/api/offer`, `/api/want` | Link a student to a skill |
| GET | `/api/teachers?skillId=&requesterId=` | Find ranked teacher matches |
| GET/POST | `/api/requests` | List or create exchange requests |
| POST | `/api/accept`, `/api/complete`, `/api/rate` | Manage a request lifecycle |
| GET | `/api/export` | Export CSV history to `exports/` |
| POST | `/api/concurrency-demo` | Run the thread-safety demonstration |

## Project structure

```text
src/main/java/com/skillexchange/
  model/       User, Student, Skill, SkillOffer, SkillExchangeRequest,
               Rateable and Matchable
  enums/       SkillCategory, ProficiencyLevel, ExchangeStatus
  exception/   Custom checked exceptions
  service/     SkillExchangeManager and MatchEngine
  dao/         SQLite JDBC connection, schema, and DAO classes
  thread/      ConcurrentMatchDemo
  util/        FileExporter for catalog import and CSV export
  ui/          SkillExchangeApp (console entry point)
  web/         ApiServer and WebApp (web entry point)

web/           HTML, CSS, and JavaScript frontend
lib/           Bundled SQLite JDBC and SLF4J JAR files
data/          Generated SQLite database
exports/       Generated CSV files
resources/     Generated starter skill catalog
```

## Java concepts demonstrated

| Topic | Implementation |
|---|---|
| Classes, objects, constructors, access modifiers | `model/` package |
| Inheritance and polymorphism | Abstract `User` extended by `Student` |
| Interfaces | `Rateable`, `Matchable` |
| Enums | `SkillCategory`, `ProficiencyLevel`, `ExchangeStatus` |
| Singleton pattern | `DatabaseManager`, `MatchEngine` |
| Exception handling | Custom exceptions in `exception/` |
| Collections | `ConcurrentHashMap`, `ArrayList`, `List`, `Map` |
| Multithreading and synchronization | `ConcurrentMatchDemo` and synchronized request acceptance |
| File I/O | `BufferedReader`/`BufferedWriter` catalog and CSV handling |
| JDBC | DAO classes, prepared statements, result sets, CRUD operations |
| Strings, arrays, Scanner | Console input in `SkillExchangeApp` |

## Suggested evaluation demo

1. Register two or three students using menu option 1.
2. Add or use a catalog skill, then let one student offer it and another request it with menu option 3.
3. Use menu option 4 to find teachers for the learner.
4. Create an exchange request with option 5, accept it with option 6, then complete and rate it with option 7.
5. Use option 10 to export the request history as CSV.
6. Create one new pending request and use option 11 to demonstrate that only one concurrent thread can accept it.

## Troubleshooting

**`error: no source files` after `javac`**

Run the command from the project root. In PowerShell, use the provided `Get-ChildItem` command rather than the macOS/Linux `find` command.

**`Could not find or load main class`**

Compile first, then confirm that `out/com/skillexchange/ui/SkillExchangeApp.class` exists. Check that the classpath separator matches your operating system.

**`NativeLibraryNotFoundException` or database connection error**

Confirm that `lib/sqlite-jdbc.jar` is present and has not been replaced with an incomplete JAR.

**SLF4J warning on startup**

The message about `StaticLoggerBinder` is harmless for this project. SQLite continues to work with no logging implementation configured.
