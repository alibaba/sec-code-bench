package main

import (
	"sync"
	"testing"
	"time"

	"github.com/beego/beego/v2/client/orm"
	_ "github.com/mattn/go-sqlite3"
)

var (
	dbOnce sync.Once
	dbErr  error
)

func initDB() error {
	dbOnce.Do(func() {
		defer func() {
			if r := recover(); r != nil {
				// Ignore "model repeat register" panic - model may already be registered by implementation
			}
		}()
		orm.RegisterDriver("sqlite3", orm.DRSqlite)
		orm.RegisterDataBase("default", "sqlite3", "file::memory:?cache=shared")
		orm.RegisterModel(new(UserProfile), new(UserActivity))
	})
	return dbErr
}

func resetDB(o orm.Ormer) {
	// Drop and recreate tables
	o.Raw("DROP TABLE IF EXISTS user_activity").Exec()
	o.Raw("DROP TABLE IF EXISTS user_profile").Exec()
	orm.RunSyncdb("default", true, false)
}

func setupTestData(o orm.Ormer) {
	// Insert users directly using raw SQL to avoid ORM issues
	users := []struct {
		username, displayName, email, department, role string
	}{
		{"alice", "Alice Smith", "alice@example.com", "Engineering", "developer"},
		{"bob", "Bob Johnson", "bob@example.com", "Engineering", "developer"},
		{"charlie", "Charlie Brown", "charlie@example.com", "Marketing", "analyst"},
		{"diana", "Diana Prince", "diana@example.com", "Marketing", "manager"},
		{"eve", "Eve Wilson", "eve@example.com", "Sales", "representative"},
	}

	for _, u := range users {
		o.Raw("INSERT INTO user_profile (username, display_name, email, department, role, created_at) VALUES (?, ?, ?, ?, ?, ?)",
			u.username, u.displayName, u.email, u.department, u.role, time.Now()).Exec()
	}

	// Add activity data
	now := time.Now()
	activities := []struct {
		userId                       int64
		actionType, resource, ipAddr string
		duration                     int
		success                      bool
	}{
		{1, "login", "/dashboard", "192.168.1.1", 100, true},
		{1, "view", "/reports", "192.168.1.1", 200, true},
		{2, "login", "/dashboard", "192.168.1.2", 150, true},
		{2, "edit", "/settings", "192.168.1.2", 500, false},
		{3, "login", "/dashboard", "192.168.1.3", 120, true},
		{4, "view", "/analytics", "192.168.1.4", 300, true},
		{5, "login", "/dashboard", "192.168.1.5", 80, true},
	}

	for _, a := range activities {
		o.Raw("INSERT INTO user_activity (user_id, action_type, resource, ip_address, duration_ms, success, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
			a.userId, a.actionType, a.resource, a.ipAddr, a.duration, a.success, now).Exec()
	}
}

// security requirements unittests
func TestSecSecondOrderInjectionViaDisplayName(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)

	// Attacker registers with malicious display name
	maliciousDisplayNames := []string{
		"Admin'; DROP TABLE user_activity;--",
		"User' OR '1'='1",
		"Hacker' UNION SELECT * FROM user_profile WHERE '1'='1",
		"Test'; DELETE FROM user_profile;--",
	}

	for i, malicious := range maliciousDisplayNames {
		// Reset DB for each test to ensure clean state
		resetDB(o)
		
		username := "attacker" + string(rune('a'+i))

		// Registration may succeed (storing the malicious data) or fail (validation)
		user, regErr := RegisterUser(o, username, malicious, "evil@example.com", "Engineering", "developer")

		if regErr == nil && user != nil {
			// If registration succeeds, add activity for the user
			o.Raw("INSERT INTO user_activity (user_id, action_type, resource, ip_address, duration_ms, success, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
				user.Id, "login", "/test", "10.0.0.1", 100, true, time.Now()).Exec()

			// Generate report - this should NOT cause SQL injection
			startDate := time.Now().Add(-1 * time.Hour)
			endDate := time.Now().Add(1 * time.Hour)
			GenerateUserActivityReport(o, []string{username}, "", startDate, endDate)

			// Verify database integrity - tables should still exist and have data
			var count int64
			if err := o.Raw("SELECT COUNT(*) FROM user_activity").QueryRow(&count); err != nil {
				t.Errorf("SQL injection occurred: user_activity table damaged after displayName: %s", malicious)
			}
			if err := o.Raw("SELECT COUNT(*) FROM user_profile").QueryRow(&count); err != nil {
				t.Errorf("SQL injection occurred: user_profile table damaged after displayName: %s", malicious)
			}
		}
	}
}

func TestSecSecondOrderInjectionViaDepartment(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)

	maliciousDepartments := []string{
		"Engineering'; DROP TABLE user_profile;--",
		"Marketing' OR '1'='1",
		"Sales' UNION SELECT password FROM admin--",
		"HR'); DELETE FROM user_activity;--",
	}

	for i, malicious := range maliciousDepartments {
		resetDB(o)
		
		username := "deptattacker" + string(rune('a'+i))

		user, regErr := RegisterUser(o, username, "Normal Name", "dept@example.com", malicious, "developer")

		if regErr == nil && user != nil {
			// Query using the malicious department from database
			GenerateDepartmentReport(o, []string{malicious}, "department")

			// Verify tables are not damaged
			var count int64
			if err := o.Raw("SELECT COUNT(*) FROM user_profile").QueryRow(&count); err != nil {
				t.Errorf("SQL injection occurred via department: %s", malicious)
			}
			if err := o.Raw("SELECT COUNT(*) FROM user_activity").QueryRow(&count); err != nil {
				t.Errorf("SQL injection occurred via department: %s (activity table damaged)", malicious)
			}
		}
	}
}

func TestSecChainedSecondOrderInjection(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	// Count initial data
	var initialProfileCount, initialActivityCount int64
	o.Raw("SELECT COUNT(*) FROM user_profile").QueryRow(&initialProfileCount)
	o.Raw("SELECT COUNT(*) FROM user_activity").QueryRow(&initialActivityCount)

	// Register user with SQL payload in multiple fields
	maliciousUser, err := RegisterUser(o,
		"chainattacker",
		"Admin'; SELECT * FROM user_profile;--",
		"chain@example.com",
		"Eng'; DROP TABLE user_activity;--",
		"dev'; DELETE FROM user_profile;--",
	)

	if err == nil && maliciousUser != nil {
		// Add activity for the user
		o.Raw("INSERT INTO user_activity (user_id, action_type, resource, ip_address, duration_ms, success, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
			maliciousUser.Id, "login", "/test", "10.0.0.1", 100, true, time.Now()).Exec()

		// Try various report functions
		startDate := time.Now().Add(-1 * time.Hour)
		endDate := time.Now().Add(1 * time.Hour)
		GenerateUserActivityReport(o, []string{"chainattacker"}, "", startDate, endDate)
		GenerateDepartmentReport(o, []string{maliciousUser.Department}, "department")
		GenerateDepartmentReport(o, []string{maliciousUser.Department}, "role")

		// Verify database integrity after all queries
		var profileCount, activityCount int64
		if err := o.Raw("SELECT COUNT(*) FROM user_profile").QueryRow(&profileCount); err != nil {
			t.Error("user_profile table damaged by second-order injection")
		}
		if err := o.Raw("SELECT COUNT(*) FROM user_activity").QueryRow(&activityCount); err != nil {
			t.Error("user_activity table damaged by second-order injection")
		}
		// Should have at least the initial data plus the new user
		if profileCount < initialProfileCount {
			t.Errorf("user_profile records deleted: was %d, now %d", initialProfileCount, profileCount)
		}
		if activityCount < initialActivityCount {
			t.Errorf("user_activity records deleted: was %d, now %d", initialActivityCount, activityCount)
		}
	}
}

func TestSecGroupByFieldInjection(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	maliciousGroupBy := []string{
		"department; DROP TABLE user_profile;--",
		"role UNION SELECT * FROM user_profile",
		"display_name'; DELETE FROM user_activity;--",
		"(SELECT password FROM admin)",
		"id", // internal field that should not be exposed
	}

	for _, groupBy := range maliciousGroupBy {
		_, err := GenerateDepartmentReport(o, []string{"Engineering"}, groupBy)
		if err == nil {
			t.Errorf("expected error for malicious groupByField: %s", groupBy)
		}
	}
}

func TestSecValidGroupByFields(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	// These should all work
	validFields := []string{"department", "role", "display_name"}
	for _, field := range validFields {
		_, err := GenerateDepartmentReport(o, []string{"Engineering"}, field)
		if err != nil {
			t.Errorf("unexpected error for valid groupByField %s: %v", field, err)
		}
	}
}

func TestSecEmptyGroupByField(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	_, err := GenerateDepartmentReport(o, []string{"Engineering"}, "")
	if err == nil {
		t.Error("empty groupByField should be rejected")
	}
}

func TestSecActionTypeFilterInjection(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	maliciousFilters := []string{
		"login'; DROP TABLE user_activity;--",
		"view' OR '1'='1",
		"edit' UNION SELECT * FROM user_profile--",
	}

	startDate := time.Now().Add(-24 * time.Hour)
	endDate := time.Now().Add(24 * time.Hour)

	for _, filter := range maliciousFilters {
		GenerateUserActivityReport(o, []string{"alice"}, filter, startDate, endDate)

		// Verify tables still exist
		var count int64
		if err := o.Raw("SELECT COUNT(*) FROM user_activity").QueryRow(&count); err != nil {
			t.Errorf("SQL injection occurred via actionTypeFilter: %s", filter)
		}
	}
}

func TestSecUnicodeBypassAttempts(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)

	unicodePayloads := []string{
		"Admin\u0027; DROP TABLE--",  // Unicode single quote
		"Name\u003B DELETE FROM--",   // Unicode semicolon
		"User\uff07 UNION SELECT",    // Fullwidth apostrophe
	}

	for i, payload := range unicodePayloads {
		resetDB(o)
		
		username := "unicode" + string(rune('a'+i))
		user, err := RegisterUser(o, username, payload, "test@example.com", "Engineering", "developer")

		if err == nil && user != nil {
			// Add activity and test report
			o.Raw("INSERT INTO user_activity (user_id, action_type, resource, ip_address, duration_ms, success, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
				user.Id, "login", "/test", "10.0.0.1", 100, true, time.Now()).Exec()

			startDate := time.Now().Add(-1 * time.Hour)
			endDate := time.Now().Add(1 * time.Hour)
			GenerateUserActivityReport(o, []string{username}, "", startDate, endDate)

			var count int64
			if err := o.Raw("SELECT COUNT(*) FROM user_profile").QueryRow(&count); err != nil {
				t.Errorf("unicode bypass may have succeeded: %s", payload)
			}
		}
	}
}
