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

// functional unittests
func TestFuncRegisterUser(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)

	user, err := RegisterUser(o, "testuser", "Test User", "test@example.com", "Engineering", "developer")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if user == nil {
		t.Fatal("expected user to be returned")
	}
	if user.Username != "testuser" {
		t.Errorf("expected username 'testuser', got '%s'", user.Username)
	}
	if user.DisplayName != "Test User" {
		t.Errorf("expected displayName 'Test User', got '%s'", user.DisplayName)
	}
	if user.Id <= 0 {
		t.Errorf("expected positive Id, got %d", user.Id)
	}
}

func TestFuncRegisterMultipleUsers(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)

	user1, err := RegisterUser(o, "user1", "User One", "one@example.com", "Engineering", "developer")
	if err != nil {
		t.Fatalf("first registration failed: %v", err)
	}

	user2, err := RegisterUser(o, "user2", "User Two", "two@example.com", "Marketing", "analyst")
	if err != nil {
		t.Fatalf("second registration failed: %v", err)
	}

	if user1.Id == user2.Id {
		t.Error("users should have different IDs")
	}
}

func TestFuncGenerateActivityReport(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	startDate := time.Now().Add(-24 * time.Hour)
	endDate := time.Now().Add(24 * time.Hour)

	results, err := GenerateUserActivityReport(o, []string{"alice", "bob"}, "", startDate, endDate)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(results) == 0 {
		t.Error("expected some activity results for alice and bob")
	}
}

func TestFuncGenerateActivityReportWithFilter(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	startDate := time.Now().Add(-24 * time.Hour)
	endDate := time.Now().Add(24 * time.Hour)

	results, err := GenerateUserActivityReport(o, []string{"alice", "bob"}, "login", startDate, endDate)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	for _, r := range results {
		if r.ActionType != "login" {
			t.Errorf("expected only login actions, got %s", r.ActionType)
		}
	}
}

func TestFuncGenerateActivityReportEmpty(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	startDate := time.Now().Add(-24 * time.Hour)
	endDate := time.Now().Add(24 * time.Hour)

	// Empty usernames should return empty results (not error)
	results, err := GenerateUserActivityReport(o, []string{}, "", startDate, endDate)
	if err != nil {
		t.Logf("empty usernames returned error (acceptable): %v", err)
	} else if len(results) > 0 {
		t.Error("empty usernames should return empty results")
	}
}

func TestFuncGenerateDepartmentReport(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	results, err := GenerateDepartmentReport(o, []string{"Engineering", "Marketing"}, "department")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(results) == 0 {
		t.Error("expected some department results")
	}
}

func TestFuncGenerateDepartmentReportByRole(t *testing.T) {
	if err := initDB(); err != nil {
		t.Fatalf("failed to init db: %v", err)
	}
	o := orm.NewOrm()
	resetDB(o)
	setupTestData(o)

	results, err := GenerateDepartmentReport(o, []string{"Engineering"}, "role")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(results) == 0 {
		t.Error("expected some role grouped results")
	}
}
