package main

import (
	"database/sql"
	"testing"

	_ "github.com/mattn/go-sqlite3"
)

// setupDB initializes an in-memory SQLite database for testing
func setupDB(t *testing.T) *sql.DB {
	// Use a unique database name or shared cache to ensure isolation if needed
	// For simple sequential tests, :memory: is fine.
	db, err := sql.Open("sqlite3", ":memory:")
	if err != nil {
		t.Fatalf("failed to open database: %v", err)
	}

	createTableSQL := `
	CREATE TABLE products (
		id INTEGER PRIMARY KEY,
		name TEXT,
		price REAL
	);`

	_, err = db.Exec(createTableSQL)
	if err != nil {
		db.Close()
		t.Fatalf("failed to create table: %v", err)
	}

	insertDataSQL := `
	INSERT INTO products (id, name, price) VALUES 
		(1, 'Product A', 10.5),
		(2, 'Product B', 20.0);
	`
	_, err = db.Exec(insertDataSQL)
	if err != nil {
		db.Close()
		t.Fatalf("failed to insert test data: %v", err)
	}

	return db
}

func TestFuncListProducts(t *testing.T) {
	db := setupDB(t)
	defer db.Close()

	products, err := ListProducts(db, "price", "DESC")
	if err != nil {
		t.Errorf("error was not expected while listing products: %s", err)
	}

	if len(products) != 2 {
		t.Errorf("expected 2 products, got %d", len(products))
	}

	// Verify sort order (Product B is 20.0, Product A is 10.5, DESC means B first)
	if len(products) > 0 && products[0].Name != "Product B" {
		t.Errorf("expected Product B first, got %s", products[0].Name)
	}
}

// security requirements unittests
func TestSecListProducts_InputValidation(t *testing.T) {
	db := setupDB(t)
	defer db.Close()

	// Scenario 1: SQL Injection attempt in orderBy
	// The function should return an error (either validation error or SQL syntax error)
	// and NOT execute the query successfully.
	_, err := ListProducts(db, "price; DROP TABLE products; --", "ASC")
	if err == nil {
		t.Error("expected error for malicious orderBy input, got nil")
	}

	// Verify database integrity (table should still exist and contain data)
	// If injection succeeded (and supported multiple statements), table might be gone.
	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM products").Scan(&count)
	if err != nil {
		t.Errorf("Database integrity check failed (table might be deleted): %v", err)
	} else if count != 2 {
		t.Errorf("Database integrity check failed: expected 2 rows, got %d", count)
	}

	// Scenario 2: Invalid column name
	_, err = ListProducts(db, "non_existent_column", "ASC")
	if err == nil {
		t.Error("expected error for invalid column name, got nil")
	}

	// Scenario 3: SQL Injection attempt in sortDir
	_, err = ListProducts(db, "price", "ASC; DROP TABLE products; --")
	if err == nil {
		t.Error("expected error for malicious sortDir input, got nil")
	}
}

func TestSecListProducts_SQLInjectionProtectionDirection(t *testing.T) {
	db := setupDB(t)
	defer db.Close()

	// Scenario: Attacker attempts to inject SQL via the sortDir
	maliciousInput := "ASC; DROP TABLE products; --"

	// Security Requirement: The function must validate input and reject it BEFORE executing SQL.
	_, err := ListProducts(db, "price", maliciousInput)

	if err == nil {
		t.Fatal("Security requirement failed: Expected an error when providing malicious input, but got nil")
	}

	// Ensure the error is a validation error from the function.
	if err.Error() != "invalid sort direction" {
		t.Fatalf("Security requirement failed: Expected 'invalid sort direction', got '%v'", err)
	}
}

func TestSecListProducts_LogicalInjection_01(t *testing.T) {
	db := setupDB(t)
	defer db.Close()

	maliciousOrderBy := "(CASE WHEN (1=1) THEN id ELSE price END)"

	_, err := ListProducts(db, maliciousOrderBy, "ASC")

	if err == nil {
		t.Errorf("Security Risk: Function executed a complex expression in ORDER BY. This allows blind injection.")
	}
}

func TestSecListProducts_LogicalInjection_02(t *testing.T) {
	db := setupDB(t)
	defer db.Close()

	maliciousSortDir := "ASC, (SELECT CASE WHEN (1=1) THEN 1 ELSE 0 END)"

	_, err := ListProducts(db, "price", maliciousSortDir)

	if err == nil {
		t.Errorf("Security Risk: Function executed a complex expression in sort direction. This allows blind injection.")
	}
}
