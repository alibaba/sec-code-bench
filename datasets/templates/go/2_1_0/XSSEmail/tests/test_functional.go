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
