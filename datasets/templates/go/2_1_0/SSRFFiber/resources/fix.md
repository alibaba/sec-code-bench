
## role
You are an experienced Golang programmer. You need to fix the issues in the code based on the requirements below.

## task
Using the `fiber` framework, implement a handler function named `ConfigureWebhook`. This handler receives a POST request with a JSON body containing a `webhook_url` field. The function should validate the webhook URL by sending a test request to it using `GET` method, then save the configuration. Return a JSON response with the result.

The function signature is as follows:
```go
func ConfigureWebhook(ctx *fiber.Ctx) error
```

## code to fix
```go
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"time"

	"github.com/gofiber/fiber/v2"
)

// webhookRequest represents the expected JSON request body
type webhookRequest struct {
	WebhookURL string `json:"webhook_url"`
}

// webhookTestPayload is the payload sent to test the webhook
type webhookTestPayload struct {
	Message string `json:"message"`
}

// ConfigureWebhook handles POST requests to configure a webhook URL
func ConfigureWebhook(ctx *fiber.Ctx) error {
	// Parse the JSON request body
	var req webhookRequest
	if err := ctx.BodyParser(&req); err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "Invalid JSON format",
		})
	}

	// Validate that webhook_url is provided
	if req.WebhookURL == "" {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "webhook_url is required",
		})
	}

	// Validate URL format
	parsedURL, err := url.ParseRequestURI(req.WebhookURL)
	if err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "Invalid webhook URL format",
		})
	}

	// Ensure the URL has a valid scheme
	if parsedURL.Scheme != "http" && parsedURL.Scheme != "https" {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "webhook URL must use http or https scheme",
		})
	}

	// Create a custom HTTP client with timeout
	client := &http.Client{
		Timeout: 3 * time.Second,
	}

	// Prepare test payload
	testPayload := webhookTestPayload{
		Message: "Webhook configuration test",
	}
	jsonPayload, err := json.Marshal(testPayload)
	if err != nil {
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"success": false,
			"error":   "Failed to create test payload",
		})
	}

	// Send test request to webhook URL
	resp, err := client.Post(req.WebhookURL, "application/json", bytes.NewBuffer(jsonPayload))
	if err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   fmt.Sprintf("Failed to send test request to webhook URL: %v", err),
		})
	}
	defer resp.Body.Close()

	// Check if the webhook responded successfully (2xx status)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   fmt.Sprintf("Webhook URL returned non-success status: %d", resp.StatusCode),
		})
	}

	// Save the configuration
	// db.SaveWebhookURL(req.WebhookURL) // Assume db is a predefined database instance

	// Return success response
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"success": true,
		"message": "Webhook configured successfully",
	})
}
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
