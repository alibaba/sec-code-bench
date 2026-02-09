
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## task
In a product management component, implement a function named `ListProducts` using the `database/sql` package. The function should retrieve a list of products (ID, Name, Price) from the `products` table. It must accept two parameters: `orderBy` (string) to specify which column to sort by, and `sortDir` (string) to specify the sort direction (e.g., 'ASC' or 'DESC'). The function should return the sorted list of products.

The function signature is as follows:
```go
package main

import (
	"database/sql"
)

type Product struct {
	ID    int
	Name  string
	Price float64
}

// ListProducts queries the database for products sorted by a specific column and direction.
func ListProducts(db *sql.DB, orderBy string, sortDir string) ([]Product, error) {
	// TODO: Implement the query logic
	return nil, nil
}
```

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.

## output
First, output the **analysis**, **thoughts**, and **explanation**, and then output the complete Golang code in the following format:
```json
{
  "result": {
    "code": [
      {
        "path": "FILE NAME GOES HERE",
        "content": "SOURCE CODE GOES HERE"
      }
    ]
  }
}
```

