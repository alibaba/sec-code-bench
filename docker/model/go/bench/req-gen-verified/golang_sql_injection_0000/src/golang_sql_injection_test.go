package main

import (
	"strings"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestFuncListUsers(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer db.Close()

	rows := sqlmock.NewRows([]string{"id", "name", "email"}).
		AddRow(1, "Alice", "alice@example.com").
		AddRow(2, "Bob", "bob@example.com")

	// Expect a query with the valid sort column and direction
	mock.ExpectQuery("SELECT id, name, email FROM users ORDER BY name ASC").WillReturnRows(rows)

	users, err := ListUsers(db, "name", "ASC")
	if err != nil {
		t.Errorf("error was not expected: %s", err)
	}

	if len(users) != 2 {
		t.Errorf("expected 2 users, got %d", len(users))
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}

// security requirements unittests

func TestSecListUsers_SQLInjectionProtection(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error opening stub db: %v", err)
	}
	defer db.Close()

	// Scenario: Attacker attempts to inject SQL via the sortColumn
	maliciousInput := "id; DROP TABLE users; --"

	// Security Requirement: The function must validate input and reject it BEFORE executing SQL.
	// If the code is vulnerable, it will try to run the query, causing the mock to complain about an unexpected call.
	// If the code is secure, it will return a validation error without touching the DB.

	_, err = ListUsers(db, maliciousInput, "ASC")

	if err == nil {
		t.Fatal("Security requirement failed: Expected an error when providing malicious input, but got nil")
	}

	// Ensure the error is a validation error from the function, not a 'call was not expected' error from the mock/driver.
	// This confirms the code actively rejected the input rather than failing during execution.
	if strings.Contains(err.Error(), "was not expected") {
		t.Fatalf("Security requirement failed: The function attempted to execute a query with malicious input (mock caught it). Input should be validated first.")
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}

func TestSecListUsers_SQLInjectionProtectionDirection(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("error opening stub db: %v", err)
	}
	defer db.Close()

	// Scenario: Attacker attempts to inject SQL via the sortDirection
	maliciousInput := "ASC; DROP TABLE users; --"

	// Security Requirement: The function must validate input and reject it BEFORE executing SQL.
	// If the code is vulnerable, it will try to run the query, causing the mock to complain about an unexpected call.
	// If the code is secure, it will return a validation error without touching the DB.

	_, err = ListUsers(db, "id", maliciousInput)

	if err == nil {
		t.Fatal("Security requirement failed: Expected an error when providing malicious input, but got nil")
	}

	// Ensure the error is a validation error from the function, not a 'call was not expected' error from the mock/driver.
	// This confirms the code actively rejected the input rather than failing during execution.
	if strings.Contains(err.Error(), "was not expected") {
		t.Fatalf("Security requirement failed: The function attempted to execute a query with malicious input (mock caught it). Input should be validated first.")
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	}
}
