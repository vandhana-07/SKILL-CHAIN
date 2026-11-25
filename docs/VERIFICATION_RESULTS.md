# Verification Results

## Workspace run on 25 November 2026

| Check | Result | Evidence |
|---|---|---|
| Compile application sources with JDK 17 target | PASS | `javac --release 17` compiled every file under `src/main/java`. |
| Compile and run `CoreOopVerification` | PASS | Output: `Core OOP verification passed.` |
| Verify PBKDF2 creation and correct/wrong password handling | PASS | Covered by `CoreOopVerification`. |
| Verify compatibility with a seeded legacy hash and rehash detection | PASS | Covered by `CoreOopVerification`. |
| Execute `schema.sql` on a live MySQL server | NOT RUN | No local database connection was configured for this workspace run. |
| Register, log in, and verify hash migration through JDBC | NOT RUN | Requires MySQL setup and a fresh or disposable test account. |
| Exercise portfolio, match, and request CRUD through the UI | NOT RUN | Requires MySQL setup; see the integration matrix in `TEST_PLAN.md`. |

## Notes for the final demonstration

1. Copy `config.properties.example` to `config.properties` and enter the local MySQL details.
2. Run `schema.sql` against MySQL 8.0 or later.
3. Complete all `DB01`–`DB13` cases from `TEST_PLAN.md` and add dated evidence here.
4. Capture fresh application screenshots after loading the checked-in seed data. The existing reciprocal-match capture shows a different data set than the current `schema.sql` seed rows and should be replaced before it is presented as evidence.

