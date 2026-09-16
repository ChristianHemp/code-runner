# Leetclone Code Runner

A small CodingBat/LeetCode-style code judge. Submit Java, it compiles and runs your
code against hidden tests, and you get back a verdict (`ACCEPTED`, `WRONG_ANSWER`,
`COMPILATION_ERROR`, `RUNTIME_ERROR`, or `TIME_LIMIT_EXCEEDED`).

- **Backend:** Java 25, Spring Boot 4, PostgreSQL
- **Frontend:** React 19, TypeScript, Vite

## Prerequisites

- JDK 25
- Node.js
- A running local PostgreSQL instance with a `codejudge` database
  (see `backend/src/main/resources/application.properties` for the connection
  details it expects)

## Run it

**Backend** (from `backend/`):

```bash
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. On first boot it seeds four problems automatically.

**Frontend** (from `frontend/`, in a separate terminal):

```bash
npm install
npm run dev
```

Runs on `http://localhost:5173`. Open that URL in your browser.

## Demo script

1. Pick a problem from the sidebar (e.g. **Sum Two Integers**) — the description,
   method signature, and starter code load automatically.
2. Click **Run solution** with the starter code as-is to see a non-`ACCEPTED`
   verdict, then fix it and submit a correct solution to see `ACCEPTED` with a
   runtime.
3. To show the other verdicts, paste one of these in for **Sum Two Integers**
   and submit:

   | Solution body | Verdict |
   |---|---|
   | `return a - b;` | `WRONG_ANSWER` |
   | `return a +` | `COMPILATION_ERROR` |
   | `throw new RuntimeException("boom");` | `RUNTIME_ERROR` |
   | `while (true) {}` | `TIME_LIMIT_EXCEEDED` |

4. Switch problems in the sidebar (**Is Palindrome**, **Maximum Element**,
   **Count Vowels**) to show the judge works across different method signatures,
   not just one hardcoded problem.

Hidden test cases and their expected outputs are never sent to the browser —
only the final verdict, runtime, and (for compile/runtime errors) diagnostic
output.
