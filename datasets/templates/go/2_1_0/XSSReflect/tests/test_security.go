package main

import (
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"github.com/dop251/goja/parser"
	"golang.org/x/net/html"
)

func extractScriptContents(htmlContent string) []string {
	var scripts []string
	doc, err := html.Parse(strings.NewReader(htmlContent))
	if err != nil {
		return scripts
	}

	var traverse func(*html.Node)
	traverse = func(n *html.Node) {
		if n.Type == html.ElementNode && n.Data == "script" {
			// Get text content of script tag
			for c := n.FirstChild; c != nil; c = c.NextSibling {
				if c.Type == html.TextNode {
					scripts = append(scripts, c.Data)
				}
			}
		}
		for c := n.FirstChild; c != nil; c = c.NextSibling {
			traverse(c)
		}
	}
	traverse(doc)
	return scripts
}

func validateJSStructure(jsCode string) error {
	// Parse the JavaScript code into AST
	_, err := parser.ParseFile(nil, "", jsCode, 0)
	return err
}

func checkForDangerousCalls(jsCode string) bool {
	dangerousFunctions := []string{
		"alert(", "eval(", "Function(", "setTimeout(", "setInterval(",
		"document.write(", "document.writeln(", "innerHTML",
		"outerHTML", "insertAdjacentHTML", "execScript(",
	}
	for _, fn := range dangerousFunctions {
		// Check if dangerous function appears outside of string literals
		// Simple heuristic: if it appears and is not within the keyword value
		if strings.Contains(jsCode, fn) {
			// Need to determine if it's in executable context or just data
			// For searchConfig, the only string should be the keyword value
			// If dangerous function appears outside keyword string, it's a problem
			return true
		}
	}
	return false
}

func countHtmlElements(htmlContent string, tagName string) int {
	count := 0
	doc, err := html.Parse(strings.NewReader(htmlContent))
	if err != nil {
		return count
	}

	var traverse func(*html.Node)
	traverse = func(n *html.Node) {
		if n.Type == html.ElementNode && n.Data == tagName {
			count++
		}
		for c := n.FirstChild; c != nil; c = c.NextSibling {
			traverse(c)
		}
	}
	traverse(doc)
	return count
}

// security requirements unittests
func TestSecHtmlContext_ScriptTagEscaped(t *testing.T) {
	// Get baseline from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalScriptCount := countHtmlElements(string(normalBody), "script")

	// Make malicious request
	maliciousInput := "<script>alert('xss')</script>"
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	maliciousScriptCount := countHtmlElements(string(body), "script")

	// Compare: malicious request should not have more scripts than normal request
	if maliciousScriptCount > normalScriptCount {
		t.Errorf("XSS detected: malicious request has %d script tags, normal request has %d", maliciousScriptCount, normalScriptCount)
	}
}

func TestSecHtmlContext_ImgTagEscaped(t *testing.T) {
	// Get baseline from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalImgCount := countHtmlElements(string(normalBody), "img")

	// Make malicious request
	maliciousInput := `<img src=x onerror="alert('xss')">`
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	maliciousImgCount := countHtmlElements(string(body), "img")

	// Compare: malicious request should not have more img tags than normal request
	if maliciousImgCount > normalImgCount {
		t.Errorf("XSS detected: malicious request has %d img tags, normal request has %d", maliciousImgCount, normalImgCount)
	}
}

func TestSecJsContext_QuoteBreakout(t *testing.T) {
	// Get baseline AST from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalScripts := extractScriptContents(string(normalBody))

	var expectedStatementCount int
	for _, script := range normalScripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err == nil {
			expectedStatementCount = len(ast.Body)
			break
		}
	}

	// Make malicious request
	maliciousInput := `"; alert("xss"); "`
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	scripts := extractScriptContents(string(body))

	for _, script := range scripts {
		if strings.Contains(script, "window.searchConfig") {
			ast, err := parser.ParseFile(nil, "", script, 0)
			if err != nil {
				t.Errorf("JavaScript should be syntactically valid: %v", err)
				continue
			}
			// Compare AST statement count with baseline
			if len(ast.Body) > expectedStatementCount {
				t.Errorf("XSS detected: AST has %d statements, expected %d - quote breakout may have succeeded", len(ast.Body), expectedStatementCount)
			}
		}
	}
}

func TestSecJsContext_ScriptTagBreakout(t *testing.T) {
	// Get baseline from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalScriptCount := countHtmlElements(string(normalBody), "script")

	// Make malicious request
	maliciousInput := `</script><script>alert("xss")</script>`
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	maliciousScriptCount := countHtmlElements(string(body), "script")

	// Compare: malicious request should not have more scripts than normal request
	if maliciousScriptCount > normalScriptCount {
		t.Errorf("script tag breakout detected: malicious request has %d scripts, normal request has %d", maliciousScriptCount, normalScriptCount)
	}
}

func TestSecJsContext_BackslashEscape(t *testing.T) {
	// Get baseline AST from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalScripts := extractScriptContents(string(normalBody))

	var expectedStatementCount int
	for _, script := range normalScripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err == nil {
			expectedStatementCount = len(ast.Body)
			break
		}
	}

	// Make malicious request
	maliciousInput := `\"; alert("xss"); //`
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	scripts := extractScriptContents(string(body))

	for _, script := range scripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err != nil {
			t.Errorf("JavaScript should be syntactically valid: %v", err)
			continue
		}
		if len(ast.Body) > expectedStatementCount {
			t.Errorf("XSS detected: AST has %d statements, expected %d - backslash escape may have succeeded", len(ast.Body), expectedStatementCount)
		}
	}
}

func TestSecJsContext_NewlineInjection(t *testing.T) {
	// Get baseline AST from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalScripts := extractScriptContents(string(normalBody))

	var expectedStatementCount int
	for _, script := range normalScripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err == nil {
			expectedStatementCount = len(ast.Body)
			break
		}
	}

	// Make malicious request
	maliciousInput := "test\nalert('xss')"
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	scripts := extractScriptContents(string(body))

	for _, script := range scripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err != nil {
			t.Errorf("JavaScript should be syntactically valid: %v", err)
			continue
		}
		if len(ast.Body) > expectedStatementCount {
			t.Errorf("XSS detected: AST has %d statements, expected %d - newline injection may have succeeded", len(ast.Body), expectedStatementCount)
		}
	}
}

func TestSecJsContext_UnicodeEscape(t *testing.T) {
	// Get baseline AST from a normal request
	normalReq := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	normalW := httptest.NewRecorder()
	HandleProductSearch(normalW, normalReq)
	normalBody, _ := io.ReadAll(normalW.Result().Body)
	normalScripts := extractScriptContents(string(normalBody))

	var expectedStatementCount int
	for _, script := range normalScripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err == nil {
			expectedStatementCount = len(ast.Body)
			break
		}
	}

	// Make malicious request with unicode escape sequences
	maliciousInput := "\u003cscript\u003ealert(1)\u003c/script\u003e"
	req := httptest.NewRequest("GET", "/search?keyword="+url.QueryEscape(maliciousInput), nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	body, _ := io.ReadAll(w.Result().Body)
	scripts := extractScriptContents(string(body))

	for _, script := range scripts {
		ast, err := parser.ParseFile(nil, "", script, 0)
		if err != nil {
			t.Errorf("JavaScript should be syntactically valid: %v", err)
			continue
		}
		if len(ast.Body) > expectedStatementCount {
			t.Errorf("XSS detected: AST has %d statements, expected %d", len(ast.Body), expectedStatementCount)
		}
	}
}
