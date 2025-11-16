package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"
)

func verifyFileName(filename string) error {
	if filename == "" {
		return nil
	}
	dangerousPatterns := []string{"..", "/", "\\", "\x00", "\r", "\n"}
	for _, pattern := range dangerousPatterns {
		if strings.Contains(filename, pattern) {
			return fmt.Errorf("forbidden pattern '%s'", pattern)
		}
	}
	if len(filename) > 255 {
		return fmt.Errorf("filename too long")
	}
	suspiciousExtensions := []string{".exe", ".bat", ".cmd", ".sh", ".ps1", ".php", ".jsp", ".asp", ".aspx", ".html", ".py", ".pyc", ".so"}
	lowerFilename := strings.ToLower(filename)
	for _, ext := range suspiciousExtensions {
		if strings.HasSuffix(lowerFilename, ext) {
			return fmt.Errorf("forbidden extension '%s'", ext)
		}
	}
	return nil
}

func (g *Gateway) forwardRequest(w http.ResponseWriter, r *http.Request, pathToSend string) {
	backendURL := os.Getenv("BACKEND_URL")
	if backendURL == "" {
		backendURL = "http://backend:5000"
	}
	backend, err := url.Parse(backendURL)
	if err != nil {
		log.Printf("Error parsing backend URL: %v", err)
		http.Error(w, "Gateway error", http.StatusInternalServerError)
		return
	}
	var reqBody io.Reader = r.Body
	if r.Method == "POST" || r.Method == "PUT" || r.Method == "PATCH" {
			r.Body = http.MaxBytesReader(nil, r.Body, 32<<20) // Limit 32MB
			bodyBytes, err := io.ReadAll(r.Body)
			if err != nil {
				log.Printf("Error reading body: %v", err)
				http.Error(w, "Request too large or invalid", http.StatusRequestEntityTooLarge)
				return
			}
			r.Body = io.NopCloser(strings.NewReader(string(bodyBytes)))
			if err := r.ParseMultipartForm(32 << 20); err != nil {
				log.Printf("Error parsing multipart form: %v", err)
				http.Error(w, "Invalid multipart data", http.StatusBadRequest)
				return
			}
			if r.MultipartForm != nil && r.MultipartForm.File != nil {
				for fieldName, fileHeaders := range r.MultipartForm.File {
					for i, fileHeader := range fileHeaders {
						fileName := fileHeader.Filename
						log.Printf("  File %d: filename=%s, size=%d", i, fileName, fileHeader.Size)
						log.Printf("Verifying file: field=%s, filename=%s", fieldName, fileName)
						if err := verifyFileName(fileName); err != nil {
							log.Printf("BLOCKED: Invalid filename '%s': %v", fileName, err)
							w.Header().Set("Content-Type", "application/json")
							w.WriteHeader(http.StatusBadRequest)
							json.NewEncoder(w).Encode(map[string]interface{}{
								"error":    "Invalid filename",
								"filename": fileName,
								"reason":   err.Error(),
							})
							return
						}
					}
				}
				log.Printf("Multipart filename verification passed")
			}
			reqBody = strings.NewReader(string(bodyBytes))
		
	}
	req, err := http.NewRequest(r.Method, "", reqBody)
	if err != nil {
		log.Printf("Error creating request: %v", err)
		http.Error(w, "Gateway error", http.StatusInternalServerError)
		return
	}
	Path := pathToSend
	if r.URL.RawQuery != "" {
		Path += "?" + r.URL.RawQuery
	}
	req.URL = &url.URL{
		Scheme: backend.Scheme,
		Host:   backend.Host,
		Opaque: Path,
	}
	req.Host = backend.Host
	log.Printf("Forwarding: %s %s", r.Method, Path)
	log.Printf("  Original path (encoded): %s", pathToSend)
	req.Header = r.Header.Clone()
	resp, err := (&http.Client{}).Do(req)
	if err != nil {
		log.Printf("Backend error: %v", err)
		http.Error(w, "Backend unavailable", http.StatusBadGateway)
		return
	}
	defer resp.Body.Close()
	for k, v := range resp.Header {
		w.Header()[k] = v
	}
	w.WriteHeader(resp.StatusCode)
	io.Copy(w, resp.Body)
	log.Printf("Response: %d", resp.StatusCode)
}

// Gateway handler
func (g *Gateway) handler(w http.ResponseWriter, r *http.Request) {
	start := time.Now()
	log.Printf("REQUEST: %s %s from %s", r.Method, r.URL.Path, r.RemoteAddr)

	username, err := g.extractUser(r)
	if err != nil {
		log.Printf("Authentication failed: %v, redirecting to login page", err)
		http.Redirect(w, r, "/gateway-login", http.StatusFound)
		return
	}
	log.Printf("User: %s", username)

	// Decode path for ACL check
	decodedPath, err := url.QueryUnescape(r.URL.Path)
	if err != nil {
		log.Printf("Decode error: %v", err)
		decodedPath = r.URL.Path
	}
	log.Printf("Path: %s -> %s cc", r.URL.Path, decodedPath)

	// Check for bypass characters in decoded path
	bypassChars := []string{"#", "?", ";", "&", "//", "\\", "..", "%00"," ", "\r", "\n"}
	for _, char := range bypassChars {
		if strings.Contains(decodedPath, char) {
			log.Printf("BLOCKED: Decoded path contains bypass character: %s", char)
			w.Header().Set("Content-Type", "application/json")
			w.WriteHeader(http.StatusBadRequest)
			json.NewEncoder(w).Encode(map[string]interface{}{
				"error":         "Invalid Path",
				"reason":        fmt.Sprintf("Path contains forbidden character: %s", char),
				"path":          decodedPath,
				"original_path": r.URL.Path,
			})
			return
		}
	}

	// Check ACL
	allowed, reason := g.checkACL(username, decodedPath, r.Method)
	if !allowed {
		log.Printf("DENIED: %s", reason)
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusForbidden)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error":         "Access Denied",
			"reason":        reason,
			"username":      username,
			"path":          decodedPath,
			"original_path": r.URL.Path,
		})
		return
	}

	log.Printf("GRANTED: %s", reason)

	// Add gateway headers
	r.Header.Set("X-Gateway-User", username)
	r.Header.Set("X-Gateway-Access", "granted")

	// Forward with original path
	g.forwardRequest(w, r, r.URL.Path)
	
	log.Printf("Completed in %v", time.Since(start))
}
