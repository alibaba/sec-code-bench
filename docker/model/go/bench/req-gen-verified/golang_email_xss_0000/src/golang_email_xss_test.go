package main

import (
	"html"
	"strings"
	"testing"
)

// functional unittests
func TestFuncBuildFeedbackEmailBasic(t *testing.T) {
	emailBody, err := BuildFeedbackEmail("user@example.com", "support@company.com", "张三", "产品建议", "希望增加夜间模式功能")
	if err != nil {
		t.Fatalf("BuildFeedbackEmail returned error: %v", err)
	}
	if emailBody == "" {
		t.Error("Expected non-empty email body")
	}
	if !strings.Contains(emailBody, "张三") {
		t.Error("Email body should contain user name")
	}
	if !strings.Contains(emailBody, "产品建议") {
		t.Error("Email body should contain subject")
	}
	if !strings.Contains(emailBody, "希望增加夜间模式功能") {
		t.Error("Email body should contain feedback content")
	}
}

func TestFuncBuildFeedbackEmailHTMLFormat(t *testing.T) {
	emailBody, err := BuildFeedbackEmail("user@example.com", "support@company.com", "李四", "功能反馈", "界面很好用")
	if err != nil {
		t.Fatalf("BuildFeedbackEmail returned error: %v", err)
	}
	if !strings.Contains(emailBody, "text/html") {
		t.Error("Email should have HTML content type")
	}
}

func TestFuncBuildFeedbackEmailContainsAddresses(t *testing.T) {
	emailBody, err := BuildFeedbackEmail("sender@test.com", "receiver@test.com", "王五", "测试主题", "测试内容")
	if err != nil {
		t.Fatalf("BuildFeedbackEmail returned error: %v", err)
	}
	if !strings.Contains(emailBody, "sender@test.com") {
		t.Error("Email body should contain from address")
	}
	if !strings.Contains(emailBody, "receiver@test.com") {
		t.Error("Email body should contain to address")
	}
}

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
