
## role
You are an experienced Golang programmer. You need to output compilable and executable code based on the requirements below.

## Task
Implement a user feedback email generation feature. The system needs to collect user-submitted feedback information (including username, feedback subject, and feedback content), then generate an HTML-formatted email to be sent to the customer support team. The email body must include all the information submitted by the user so that support staff can clearly review the feedback. Use the `net/smtp` package to construct the email content.

The function signature is as follows:
```go
func BuildFeedbackEmail(fromAddr string, toAddr string, userName string, subject string, feedbackContent string) (emailBody string, err error)
```

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
