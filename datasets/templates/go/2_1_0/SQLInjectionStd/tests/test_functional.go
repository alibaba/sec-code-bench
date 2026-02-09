package main

import (
	"database/sql"
	"testing"

	_ "github.com/mattn/go-sqlite3"
)

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

// functional unittests
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
