# Skill Chain

Skill Chain is a Java Swing desktop application for reciprocal skill exchange among campus students. A student lists skills to teach and skills to learn; the matching screen finds peers where both sides have something the other wants.

## What it demonstrates

- Java 17, Swing, packages, and the OOP concepts in the project rubric.
- MySQL relational design: `users`, `skills`, `user_skills`, and `learning_requests`.
- JDBC CRUD workflows using DAO interfaces, prepared statements, and try-with-resources.
- Reciprocal matching with a four-way join across student skill records.
- Domain exceptions, database error handling, and password-hash migration.

See [`docs/FINAL_PROJECT_REPORT.md`](docs/FINAL_PROJECT_REPORT.md) for the report organized to the college's required structure. See [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md) for the verification record and remaining live-MySQL checks.

## Requirements

- JDK 17 or later
- MySQL 8.0 or later
- Maven (optional; Windows batch scripts are also provided)

## Run locally

1. Run `schema.sql` in MySQL. The script creates the database, tables, indexes, constraints, and demo data. It also widens `users.password_hash` for the current encoded PBKDF2 value on databases created by an earlier version.
2. Copy `config.properties.example` to `config.properties` and set `db.url`, `db.user`, and `db.password`. The local configuration file is ignored by Git.
3. Start the app with `mvn exec:java`, or use `run.bat` on Windows.

The four seeded demo accounts share the password `Password123!`:

| Student | Login | Example exchange |
|---|---|---|
| Alice Johnson | `alice@university.edu` | Teaches Java; wants Python |
| Bob Smith | `bob@university.edu` | Teaches Python; wants Java |
| Charlie Davis | `charlie@university.edu` | Teaches React; wants SQL |
| Diana Prince | `diana@university.edu` | Teaches SQL; wants React |

These credentials are for the classroom demonstration only. Do not reuse them for a real account.

## Code map

| Package | Responsibility |
|---|---|
| `model` | Encapsulated domain objects and the `Person` abstraction |
| `dao` | Persistence contracts and JDBC implementations |
| `service` | Registration, skill workflows, matching, and requests |
| `ui` | Swing screens and interaction |
| `util` | Database configuration, password hashing, and domain exceptions |

`User` and `PeerStudent` inherit from abstract `Person`. `User` overrides abstract methods; skill search and portfolio methods are overloaded. The DAO interfaces keep services independent of JDBC implementations. Checked domain exceptions distinguish duplicate records and invalid credentials, while `SQLException` is handled at the UI boundary.

## Password handling

New registrations use PBKDF2-HMAC-SHA256 with a unique 16-byte random salt and a stored work factor. Earlier demo accounts remain compatible: after a successful login, their legacy salted SHA-256 hash is replaced with the current format. The schema widens the hash column for this encoded value.

## Report and evidence

- [`docs/FINAL_PROJECT_REPORT.md`](docs/FINAL_PROJECT_REPORT.md): chapters 1–8, references, and appendices.
- [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md): executable smoke-check steps and database test matrix.
- [`docs/VERIFICATION_RESULTS.md`](docs/VERIFICATION_RESULTS.md): checks run in this workspace and checks that still need a configured MySQL server.
- [`screenshots/`](screenshots/): application output screens.

The file `screenshots/07_Reciprocal_Matches_STALE_DO_NOT_USE.png` shows a different data set than the checked-in seed. Regenerate it after loading `schema.sql` before using it as final evidence.
