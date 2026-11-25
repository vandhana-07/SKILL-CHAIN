# Test Plan and Results

## Environment

- JDK 17 or later
- MySQL 8.0 or later for database integration tests
- Database initialized from `schema.sql`
- Local credentials in `config.properties`

## Database independent smoke check

This check compiles the application and exercises the OOP, validation, category traversal, and password utility behavior without requiring MySQL.

From the repository root, run:

```bash
rm -rf /tmp/skill-chain-test-classes
mkdir -p /tmp/skill-chain-test-classes
find src/main/java -name '*.java' -print0 | xargs -0 javac --release 17 -d /tmp/skill-chain-test-classes
javac --release 17 -cp /tmp/skill-chain-test-classes -d /tmp/skill-chain-test-classes \
  src/test/java/com/skillchain/verification/CoreOopVerification.java
java -ea -cp /tmp/skill-chain-test-classes com.skillchain.verification.CoreOopVerification
```

Expected output:

```text
Core OOP verification passed.
```

## Live MySQL integration matrix

Run these cases in a disposable local database. Record the date, account, inputs, actual result, and screenshot or SQL output in `VERIFICATION_RESULTS.md`.

| ID | Test | Expected result |
|---|---|---|
| DB01 | Execute `schema.sql` | Four tables, keys, checks, indexes, and sample rows are created. |
| DB02 | Register a new student | One `users` row is added with a PBKDF2 hash and salt. |
| DB03 | Log in with a seeded account | Login succeeds; the legacy hash is replaced with PBKDF2. |
| DB04 | Add and view a `TEACH` or `LEARN` skill | The portfolio row appears for the signed-in student. |
| DB05 | Search the skill catalogue and portfolio | Results match keyword and category filters. |
| DB06 | Remove a portfolio entry | Only the signed-in student's selected row is deleted. |
| DB07 | Log in as Alice or Bob and open matches | The seeded Java/Python reciprocal exchange is displayed. |
| DB08 | Log in as Charlie or Diana and open matches | The seeded React/SQL reciprocal exchange is displayed. |
| DB09 | Send an exchange proposal | A `PENDING` row appears in the sender's outgoing and receiver's incoming lists. |
| DB10 | Accept as the addressed receiver | The request changes to `ACCEPTED`. |
| DB11 | Attempt to accept as another user or reprocess a completed request | The request remains unchanged. |
| DB12 | Reject a pending request as its receiver | The request changes to `REJECTED`. |
| DB13 | Stop MySQL and refresh a database-backed screen | The UI displays a controlled connection error. |

Do not mark a case passed until it has been run against MySQL and evidence has been recorded. The latest workspace run is summarized in `VERIFICATION_RESULTS.md`.
