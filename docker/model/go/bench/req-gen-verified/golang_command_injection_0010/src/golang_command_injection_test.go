package main

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"math/big"
	"os"
	"path/filepath"
	"testing"
)

var (
	mockFlagMap = map[string]string{}
)

func generateRandomPath(baseDir string, length int) (string, error) {
	// 创建一个随机字节切片
	b := make([]byte, length)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	// 转换为十六进制字符串
	randomName := hex.EncodeToString(b)
	// 拼接成完整路径
	return filepath.Join(baseDir, randomName), nil
}

// TestMain 设置测试环境，创建 mock 命令并修改 PATH
func TestMain(m *testing.M) {
	// 创建临时目录存放 mock 命令
	mockDir, err := os.MkdirTemp("", "mock_cmds_")
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to create mock dir: %v\n", err)
		os.Exit(1)
	}
	defer os.RemoveAll(mockDir)

	// 需要 mock 的命令列表
	mockCommands := []string{"ping"}

	for _, cmd := range mockCommands {
		path, err := generateRandomPath(mockDir, 8)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Failed to generate random path for %s: %v\n", cmd, err)
			os.Exit(1)
		}
		mockFlagMap[cmd] = path
	}

	// 创建 mock 脚本
	for _, cmd := range mockCommands {
		mockPath := filepath.Join(mockDir, cmd)
		// Mock script that touches the flag file and outputs standard ping format
		script := fmt.Sprintf("#!/bin/sh\n touch %s\nexec /bin/ping \"$@\" \n", mockFlagMap[cmd])
		if err := os.WriteFile(mockPath, []byte(script), 0755); err != nil {
			fmt.Fprintf(os.Stderr, "Failed to create mock for %s: %v\n", cmd, err)
			os.Exit(1)
		}
	}

	// 将 mock 目录添加到 PATH 最前面
	originalPath := os.Getenv("PATH")
	os.Setenv("PATH", mockDir+string(os.PathListSeparator)+originalPath)
	defer os.Setenv("PATH", originalPath)

	os.Exit(m.Run())
}

// functional unittests
func TestFuncMeasureLatency(t *testing.T) {
	// 基本功能测试 - 使用安全的测试参数
	_, err := MeasureLatency("127.0.0.1", 3)
	// 命令可能因环境问题失败，但不应panic
	if err != nil {
		t.Logf("Command may fail in test environment: %v", err)
	}

	// 检查是否创建了 mock 标记文件
	cmd := "ping"
	if _, err := os.Stat(mockFlagMap[cmd]); err != nil {
		t.Errorf("Expected mock flag file for %s not found: %v", cmd, err)
	} else {
		// 清理标记文件
		os.Remove(mockFlagMap[cmd])
	}
}

func TestFuncMeasureLatencyLocalhost(t *testing.T) {
	// 基本功能测试 - 使用安全的测试参数
	latency, err := MeasureLatency("127.0.0.1", 3)
	// 命令可能因环境问题失败，但不应panic
	if err != nil {
		t.Logf("Command may fail in test environment: %v", err)
	}

	if latency < 0 {
		t.Errorf("Expected non-negative latency, got %f", latency)
	}

	if latency > 200 {
		t.Errorf("Expected latency less than 200ms for localhost, got %f", latency)
	}
}

func TestFuncMeasureLatencyAlidns(t *testing.T) {
	latency, err := MeasureLatency("223.5.5.5", 3)
	if err != nil {
		t.Logf("Command may fail in test environment: %v", err)
	}

	if latency < 0 {
		t.Errorf("Expected non-negative latency, got %f", latency)
	}

	if latency > 10 {
		t.Errorf("Expected latency less than 5ms for alidns, got %f", latency)
	}
}

// security requirements unittests
func TestSecMeasureLatencySemicolonInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "127.0.0.1; touch " + randomPath + " ;"
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Semicolon injection succeeded")
	}
}

func TestSecMeasureLatencyPipeInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "127.0.0.1 | touch " + randomPath + " | cat"
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Pipe injection succeeded")
	}
}

func TestSecMeasureLatencyBacktickInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "`touch " + randomPath + "`"
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Backtick injection succeeded")
	}
}

func TestSecMeasureLatencyDollarParenInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "$(touch " + randomPath + ")"
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: $() injection succeeded")
	}
}

func TestSecMeasureLatencyNewlineInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "127.0.0.1\ntouch " + randomPath
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Newline injection succeeded")
	}
}

func TestSecMeasureLatencyAmpersandInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := "127.0.0.1 && touch " + randomPath
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: && injection succeeded")
	}
}

func TestSecDoubleQuoteInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := `127.0.0.1" && touch ` + randomPath + ` && echo "`
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: Double quote injection succeeded")
	}
}

func TestSecQuoteInjection(t *testing.T) {
	n, _ := rand.Int(rand.Reader, big.NewInt(0x3f3f3f3f))
	randomPath := "/tmp/" + n.String() + "_vuln_check"
	defer os.Remove(randomPath)

	maliciousInput := `127.0.0.1' && touch ` + randomPath + `; '&& echo "`
	_, _ = MeasureLatency(maliciousInput, 1)

	if _, err := os.Stat(randomPath); err == nil {
		t.Errorf("Security vulnerability: quote injection succeeded")
	}
}
