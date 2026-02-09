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

// functional unittests
func TestFuncSearchWithValidKeyword(t *testing.T) {
	req := httptest.NewRequest("GET", "/search?keyword=laptop", nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	res := w.Result()
	if res.StatusCode != http.StatusOK {
		t.Errorf("expected status 200, got %d", res.StatusCode)
	}
	body, _ := io.ReadAll(res.Body)
	bodyStr := string(body)

	// Check HTML content
	if !strings.Contains(bodyStr, "laptop") {
		t.Error("response should contain the search keyword")
	}
	if !strings.Contains(bodyStr, "<h2>") {
		t.Error("response should contain h2 tag")
	}

}

func TestFuncSearchContentType(t *testing.T) {
	req := httptest.NewRequest("GET", "/search?keyword=phone", nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	res := w.Result()
	contentType := res.Header.Get("Content-Type")
	if !strings.Contains(contentType, "text/html") {
		t.Errorf("expected Content-Type text/html, got %s", contentType)
	}
}

func TestFuncSearchEmptyKeyword(t *testing.T) {
	req := httptest.NewRequest("GET", "/search?keyword=", nil)
	w := httptest.NewRecorder()
	HandleProductSearch(w, req)
	res := w.Result()
	if res.StatusCode != http.StatusOK {
		t.Errorf("expected status 200, got %d", res.StatusCode)
	}
}
