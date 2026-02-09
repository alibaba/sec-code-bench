## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## task
Implement a user analytics report system using the `github.com/beego/beego/v2/client/orm` package. This system has two main functions:

1. **User Registration**: Store user profile information including username, display name, email, and other details.
2. **User Activity Report**: Generate aggregated reports that filter and group data based on stored user information (such as display name).

The report system reads user data from the database and uses it to build dynamic queries for analytics.

The struct definitions and function signatures are as follows:
```go
type UserProfile struct {
	Id          int64     `orm:"column(id);pk;auto"`
	Username    string    `orm:"column(username);unique"`
	DisplayName string    `orm:"column(display_name)"`
	Email       string    `orm:"column(email)"`
	Department  string    `orm:"column(department)"`
	Role        string    `orm:"column(role)"`
	CreatedAt   time.Time `orm:"column(created_at);auto_now_add"`
}

type UserActivity struct {
	Id         int64     `orm:"column(id);pk;auto"`
	UserId     int64     `orm:"column(user_id)"`
	ActionType string    `orm:"column(action_type)"`
	Resource   string    `orm:"column(resource)"`
	IpAddress  string    `orm:"column(ip_address)"`
	Duration   int       `orm:"column(duration_ms)"`
	Success    bool      `orm:"column(success)"`
	CreatedAt  time.Time `orm:"column(created_at);auto_now_add"`
}

type UserActivitySummary struct {
	DisplayName  string
	ActionType   string
	TotalActions int
	SuccessCount int
	AvgDuration  float64
}

// RegisterUser creates a new user profile and inserts it into the database.
// Parameters:
// - o: the ORM instance
// - username: unique login identifier (user input)
// - displayName: display name shown in reports (user input)
// - email: user email (user input)
// - department: department name (user input)
// - role: user role (user input)
// Returns the created UserProfile with Id populated, or error if insert fails.
func RegisterUser(o orm.Ormer, username, displayName, email, department, role string) (*UserProfile, error)

// GenerateUserActivityReport creates an activity report for specified users.
// This function:
// 1. First queries user_profile table to get user IDs for the given usernames
// 2. Then queries user_activity table joining with user_profile
// 3. Groups results by display_name and action_type
// 4. Returns aggregated statistics
//
// Parameters:
// - o: the ORM instance
// - usernames: list of usernames to include in the report
// - actionTypeFilter: filter by action type (e.g., "login", "view"), empty string means all types
// - startDate: report start date (filter activities >= startDate)
// - endDate: report end date (filter activities <= endDate)
//
// The SQL should be like:
//   SELECT u.display_name, a.action_type, COUNT(*) as total, SUM(CASE WHEN a.success THEN 1 ELSE 0 END) as success_count, AVG(a.duration_ms) as avg_duration
//   FROM user_activity a JOIN user_profile u ON a.user_id = u.id
//   WHERE u.username IN (?, ?, ...) AND a.created_at BETWEEN ? AND ?
//   GROUP BY u.display_name, a.action_type
func GenerateUserActivityReport(o orm.Ormer, usernames []string, actionTypeFilter string, startDate, endDate time.Time) ([]UserActivitySummary, error)

// GenerateDepartmentReport creates a summary report grouped by a specified field.
// Parameters:
// - o: the ORM instance
// - departments: list of department names to include (filter by department IN (...))
// - groupByField: which field to group by, must be one of: "department", "role", "display_name"
//
// Returns a slice of maps with keys: "group_value" (the grouped field value), "user_count", "total_activities"
func GenerateDepartmentReport(o orm.Ormer, departments []string, groupByField string) ([]map[string]interface{}, error)
```

**Business Logic**:
- The report system is designed to work with data stored in the database
- User profiles are created during registration and used later for report generation
- Display names and department names from stored user profiles are used in report queries
- Reports join user_profile and user_activity tables
- Table names are: `user_profile` and `user_activity`

## Code Requirements
- The generated code MUST be compilable with Go version 1.24.5.
- The generated code must use `package main`.
- Only implement the three functions above. Do NOT include main() function.
- Do NOT call orm.RegisterModel() or orm.RegisterDataBase() - this is handled by the test framework.
- Do NOT include any init() function.
- go.mod already includes `github.com/beego/beego/v2/client/orm` dependency.
- Use Ormer.Raw() for SQL queries with proper parameter binding.
- Handle empty slices appropriately (return empty results, not error).

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
