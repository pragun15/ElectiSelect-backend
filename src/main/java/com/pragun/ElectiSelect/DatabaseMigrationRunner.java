package com.pragun.ElectiSelect;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 Checking and restoring foreign key constraints...");

        // Migrate new columns for profile completion
        try {
            jdbcTemplate.execute("ALTER TABLE app_users ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN NOT NULL DEFAULT false");
            jdbcTemplate.execute("ALTER TABLE app_users ADD COLUMN IF NOT EXISTS phone VARCHAR(20)");
            System.out.println("✅ Ensured 'profile_completed' and 'phone' columns exist on app_users.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not add profile columns: " + e.getMessage());
        }

        try {
            // 1. Recreate foreign key for subject
            jdbcTemplate.execute(
                "ALTER TABLE subject DROP CONSTRAINT IF EXISTS fk_subject_session"
            );
            jdbcTemplate.execute(
                "ALTER TABLE subject ADD CONSTRAINT fk_subject_session FOREIGN KEY (session_id) REFERENCES sessions(id)"
            );
            System.out.println("✅ Restored foreign key for 'subject' table.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not restore FK for 'subject': " + e.getMessage());
        }

        try {
            // 2. Recreate foreign key for dept_category
            jdbcTemplate.execute(
                "ALTER TABLE dept_category DROP CONSTRAINT IF EXISTS fk_dept_category_session"
            );
            jdbcTemplate.execute(
                "ALTER TABLE dept_category ADD CONSTRAINT fk_dept_category_session FOREIGN KEY (session_id) REFERENCES sessions(id)"
            );
            System.out.println("✅ Restored foreign key for 'dept_category' table.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not restore FK for 'dept_category': " + e.getMessage());
        }

        try {
            // 3. Recreate foreign key for dept_elective_selection
            jdbcTemplate.execute(
                "ALTER TABLE dept_elective_selection DROP CONSTRAINT IF EXISTS fk_dept_selection_session"
            );
            jdbcTemplate.execute(
                "ALTER TABLE dept_elective_selection ADD CONSTRAINT fk_dept_selection_session FOREIGN KEY (session_id) REFERENCES sessions(id)"
            );
            System.out.println("✅ Restored foreign key for 'dept_elective_selection' table.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not restore FK for 'dept_elective_selection': " + e.getMessage());
        }

        try {
            // 4. Recreate foreign key for open_elective_selection
            jdbcTemplate.execute(
                "ALTER TABLE open_elective_selection DROP CONSTRAINT IF EXISTS fk_open_selection_session"
            );
            jdbcTemplate.execute(
                "ALTER TABLE open_elective_selection ADD CONSTRAINT fk_open_selection_session FOREIGN KEY (session_id) REFERENCES sessions(id)"
            );
            System.out.println("✅ Restored foreign key for 'open_elective_selection' table.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not restore FK for 'open_elective_selection': " + e.getMessage());
        }

        try {
            // 5. Clean duplicates and add unique constraint for open_elective_selection
            jdbcTemplate.execute(
                "DELETE FROM open_elective_selection WHERE id NOT IN (SELECT MIN(id) FROM open_elective_selection GROUP BY student_id)"
            );
            System.out.println("✅ Cleaned duplicate entries in 'open_elective_selection' table.");
            
            jdbcTemplate.execute(
                "ALTER TABLE open_elective_selection ADD CONSTRAINT unique_student_selection UNIQUE (student_id)"
            );
            System.out.println("✅ Added unique constraint on student_id to 'open_elective_selection' table.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not clean duplicates or add unique constraint: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE subject DROP COLUMN IF EXISTS allowed_depts");
            System.out.println("✅ Dropped 'allowed_depts' column from 'subject' table (blocklist-only mode).");
        } catch (Exception e) {
            System.out.println("⚠️ Could not drop 'allowed_depts' column: " + e.getMessage());
        }

        try {
            // Fix NULL boolean values
            jdbcTemplate.execute("UPDATE subject SET is_deleted = false WHERE is_deleted IS NULL");
            System.out.println("✅ Fixed NULL values for subject.is_deleted");
            jdbcTemplate.execute("UPDATE sessions SET is_active = false WHERE is_active IS NULL");
            System.out.println("✅ Fixed NULL values for sessions.is_active");
        } catch (Exception e) {
            System.out.println("⚠️ Could not update NULL boolean values: " + e.getMessage());
        }

        try {
            // Fix the legacy 'deleted' column: set default and repair NULLs
            jdbcTemplate.execute("ALTER TABLE subject ALTER COLUMN deleted SET DEFAULT false");
            jdbcTemplate.execute("UPDATE subject SET deleted = false WHERE deleted IS NULL");
            System.out.println("✅ Fixed legacy 'deleted' column: set default=false and repaired NULLs.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not fix legacy 'deleted' column (it may not exist): " + e.getMessage());
        }
        
        try {
            // Fix the test user role so they don't get 403 Forbidden
            jdbcTemplate.execute("UPDATE app_users SET role = 'STUDENT', department = 'ISE' WHERE email = '1ds24is110@dsce.edu.in'");
            System.out.println("✅ Reset test user '1ds24is110@dsce.edu.in' to STUDENT role with ISE department.");

            // Fix their academic state
            jdbcTemplate.execute("UPDATE academic_state SET current_semester = 5, is_eligible = true WHERE user_id = (SELECT id FROM app_users WHERE email = '1ds24is110@dsce.edu.in')");
            System.out.println("✅ Set test user to semester 5 and eligible.");

            // Step 1: Ensure an active OPEN session exists for semester 5
            Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE semester = 5 AND is_active = true AND type = 'OPEN'", Integer.class);
            if (sessionCount == null || sessionCount == 0) {
                jdbcTemplate.execute("INSERT INTO sessions (type, semester, academic_year, is_active, start_time, end_time) VALUES ('OPEN', 5, '2023-2024', true, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '10 days')");
                System.out.println("✅ Created a test active OPEN session for Semester 5.");
            } else {
                System.out.println("✅ Active OPEN session for Semester 5 already exists.");
            }

            // Step 2: Get the session ID
            Long sessionId = jdbcTemplate.queryForObject(
                "SELECT id FROM sessions WHERE semester = 5 AND is_active = true AND type = 'OPEN' LIMIT 1", Long.class);
            System.out.println("✅ Using session id=" + sessionId + " for subject seeding.");

            // Step 3: Ensure subjects exist for this session
            Integer subjectCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subject WHERE session_id = " + sessionId, Integer.class);
            if (subjectCount == null || subjectCount == 0) {
                jdbcTemplate.execute("INSERT INTO subject (session_id, course_code, title, department, max_seats, filled_seats, is_deleted, deleted) VALUES (" + sessionId + ", 'AI101', 'Introduction to Artificial Intelligence', 'CSE', 60, 0, false, false)");
                jdbcTemplate.execute("INSERT INTO subject (session_id, course_code, title, department, max_seats, filled_seats, is_deleted, deleted) VALUES (" + sessionId + ", 'DS101', 'Data Science Fundamentals', 'ISE', 60, 0, false, false)");
                jdbcTemplate.execute("INSERT INTO subject (session_id, course_code, title, department, max_seats, filled_seats, is_deleted, deleted) VALUES (" + sessionId + ", 'ML101', 'Machine Learning Basics', 'CSE', 60, 0, false, false)");
                System.out.println("✅ Created 3 test subjects linked to session " + sessionId + ".");
            } else {
                System.out.println("✅ Subjects already exist for session " + sessionId + " (count=" + subjectCount + "). Skipping seed.");
            }

            // Step 4: Print subjects in DB for debug
            System.out.println("📋 All subjects in DB for session " + sessionId + ":");
            jdbcTemplate.queryForList("SELECT id, course_code, title, department, is_deleted FROM subject WHERE session_id = " + sessionId)
                .forEach(row -> System.out.println("  -> " + row));

        } catch (Exception e) {
            System.out.println("⚠️ Could not update test user or create dummy data: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("🔄 Database schema fix complete. You can safely delete DatabaseMigrationRunner.java if no longer needed.");
    }
}
