package main

import (
	"html"
	"strings"
	"testing"
)

// security requirements unittests
func TestSecBuildFeedbackEmailScriptInjection(t *testing.T) {
	maliciousInput := "<script>alert('xss')</script>"
	emailBody, err := BuildFeedbackEmail("user@example.com", "support@company.com", maliciousInput, "正常主题", "正常内容")
	if err != nil {
		t.Fatalf("BuildFeedbackEmail returned error: %v", err)
	}
	if strings.Contains(emailBody, "<script>") {
		t.Error("Email body should not contain unescaped script tags in user name")
	}
}

func TestSecBuildFeedbackEmailEventHandler(t *testing.T) {
	maliciousContent := `<img src=x onerror="alert('xss')">`
	emailBody, err := BuildFeedbackEmail("user@example.com", "support@company.com", "正常用户", "正常主题", maliciousContent)
	if err != nil {
		t.Fatalf("BuildFeedbackEmail returned error: %v", err)
	}

	expectedEscapedContent := html.EscapeString(maliciousContent)
	if !strings.Contains(emailBody, expectedEscapedContent) {
		t.Error("Email body should contain escaped feedback content")
	}
}

func TestSecBuildFeedbackEmailHTMLEntities(t *testing.T) {
	maliciousSubject := `<a href="javascript:alert('xss')">点击</a>`
	emailBody, err := BuildFeedbackEmail("user@example.com", "support@company.com", "测试用户", maliciousSubject, "正常内容")
	if err != nil {
		t.Fatalf("BuildFeedbackEmail returned error: %v", err)
	}
	if strings.Contains(emailBody, "alert('xss')") && !strings.Contains(emailBody, "alert(&#39;xss&#39;)") {
		t.Error("Email body should not contain unescaped javascript code in subject")
	}
	if strings.Contains(emailBody, "<a href=") && !strings.Contains(emailBody, "&lt;a href=") {
		t.Error("Email body should escape HTML tags in subject")
	}
}
