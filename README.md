# Kilasy

Kilasy is a programming exercise platform. Teachers publish exercises with a deadline, students submit their code or a
link to their GitHub repository, and teachers then grade them. Students receive their grade by email.

Production version: 1.3.0.

## Stack

- Java 21, Spring Boot 4 (Web MVC, JDBC, Mail)
- PostgreSQL 17
- Maven

## Getting started

Pre-requisites: JDK 21, Maven and Docker (see the installation guide on hei-lms.app).

```bash
docker compose up -d
mvn spring-boot:run
```

The API responds on http://localhost:8080.

`docker compose` starts two services:

- PostgreSQL on port 5432;
- Mailpit, a fake mail server: emails sent by Kilasy are displayed at http://localhost:8025.

**The SQL scripts are idempotent**: at every startup the schema and the demo data are created only if missing, and
existing data is kept. To start from scratch, run `docker compose down` (PostgreSQL has no persistent volume, so
removing the container wipes the data). The demo data is: 2 teachers, 6 students, 5 exercises and 12 submissions.

## API

| Method | Path                          | Purpose                                  |
|--------|-------------------------------|------------------------------------------|
| GET    | `/exercises`                  | List exercises                           |
| GET    | `/exercises/{id}`             | Exercise details                         |
| POST   | `/exercises`                  | Create an exercise (teacher)             |
| POST   | `/exercises/{id}/submissions` | Submit an exercise (student)             |
| GET    | `/submissions/{id}`           | Submission details                       |
| PUT    | `/submissions/{id}/grade`     | Grade a submission (teacher)             |
| GET    | `/students`                   | List students                            |
| GET    | `/students/{id}/transcript`   | A student's grade transcript             |
| GET    | `/exports/grades.csv`         | Grades export for the registrar's office |

There is no authentication yet: the student or teacher identifier is passed in the request body.

## Examples

Create an exercise:

```bash
curl -X POST http://localhost:8080/exercises \
  -H "Content-Type: application/json" \
  -d '{"title": "Recherche dichotomique", "maxPoints": 20, "deadline": "2027-03-01", "teacherId": 1}'
```

Submit an exercise:

```bash
curl -X POST http://localhost:8080/exercises/4/submissions \
  -H "Content-Type: application/json" \
  -d '{"studentId": 2, "repoUrl": "https://github.com/bob/reservation-api"}'
```

Grade a submission, then check the email received at http://localhost:8025:

```bash
curl -X PUT http://localhost:8080/submissions/5/grade \
  -H "Content-Type: application/json" \
  -d '{"points": 15, "feedback": "Bien, mais commente ton code."}'
```

View a grade transcript and the export:

```bash
curl http://localhost:8080/students/1/transcript
curl http://localhost:8080/exports/grades.csv
```

## Business rules

- A submission contains code, a link to a GitHub repository, or both.
- A student can only submit once per exercise.
- A submission sent after the deadline day is marked as late.
- Points are entered on the exercise's scale (10, 20, 40…) and converted to a grade out of 20.

## Version 1.3.1 – Late Submission Grade Penalty

### Overview

Late submissions receive a **10% penalty per started day**, up to a maximum of **50%** of the original grade.

Example (original grade: 16/20):

| Submission time               | Final grade |
|-------------------------------|-------------|
| On time                       | 16.0        |
| 2 hours late                  | 14.4        |
| 3 days late                   | 11.2        |
| 8 days late (maximum penalty) | 8.0         |

### Implementation

- **GradeCalculator:** A new class centralizes all grade calculations, including late status, late days, penalties, and
  final grades.
- **Consistent results:** The same calculation is used for submission details, grading, student transcripts, and CSV
  exports.
- **No database changes:** Existing database fields are sufficient to calculate late penalties.
- **Unified rounding:** All grades are rounded to one decimal place using `HALF_UP`.
- **Late detection:** The deadline date is not considered late. Each following calendar day counts as one started day.
- **Grade protection:** Final grades cannot be negative.
- **Email notifications:** Students receive their penalized grade and the applied penalty by email.
- **API updates:** The grading endpoint now returns `daysLate` and `penaltyPercent`. Submission details also include
  `maxPoints`. These changes are backward-compatible.

### Modified Files

```text
GradeCalculator.java (new)
SubmissionController.java
GradeReport.java
CsvExport.java
```

The following files remain unchanged: `Exercise.java`, `ExerciseController.java`, `StudentController.java`, and
`schema.sql`.

### Tests

- `GradeCalculatorTest.java`: 13 tests covering penalties, rounding, different grading scales, and grade limits.
- `GradeConsistencyTest.java`: 1 test verifying that the same penalized grade is displayed in the grading interface,
  transcript, CSV export, and email.

### Run Tests

```bash
mvn clean test
```

**Result:** 14 tests passed, 0 failures, 0 errors, 0 skipped.