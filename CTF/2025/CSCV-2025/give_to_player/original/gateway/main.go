package main

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type User struct {
	Username string   `json:"username"`
	Roles    []string `json:"roles"`
}

type ACLRule struct {
	Path        string   `json:"path"`
	Methods     []string `json:"methods"`
	RequiredRole string  `json:"required_role"`
}

type Gateway struct {
	users     map[string]User
	aclRules  []ACLRule
	jwtSecret string
}

// Initialize users and ACL rules
func (g *Gateway) initializeData() {
	// Mock users database
	g.users = map[string]User{
		"admin": {
			Username: "admin",
			Roles:    []string{"admin", "user"},
		},
		"user1": {
			Username: "user1",
			Roles:    []string{"user"},
		},
		"guest": {
			Username: "guest",
			Roles:    []string{"guest"},
		},
	}

	// ACL Rules - Order matters! More specific rules should come first
	g.aclRules = []ACLRule{
		{
			Path:        "/admin",
			Methods:     []string{"GET", "POST", "PUT", "DELETE"},
			RequiredRole: "admin",
		},
		{
			Path:        "/uploads",
			Methods:     []string{"GET", "POST", "PUT", "DELETE"},
			RequiredRole: "admin", // Allow all users (guest is the default minimum role)
		},
		{
			Path:        "/",
			Methods:     []string{"GET"},
			RequiredRole: "guest",
		},
	}
}

// Verify JWT token and return claims
func (g *Gateway) verifyToken(tokenString string) (jwt.MapClaims, error) {
	// Validate token length to prevent DoS
	if len(tokenString) > 2048 {
		return nil, fmt.Errorf("token too long")
	}
	
	// Parse and verify JWT
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		// Verify signing method - CRITICAL security check
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		
		// Only allow HS256
		if token.Method.Alg() != "HS256" {
			return nil, fmt.Errorf("only HS256 algorithm is allowed")
		}
		
		return []byte(g.jwtSecret), nil
	})
	
	if err != nil {
		return nil, err
	}
	
	if !token.Valid {
		return nil, fmt.Errorf("token is invalid")
	}
	
	// Extract claims
	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok {
		return nil, fmt.Errorf("failed to extract claims")
	}
	
	// Verify issuer
	iss, ok := claims["iss"].(string)
	if !ok || iss != "gateway" {
		return nil, fmt.Errorf("invalid issuer")
	}
	
	return claims, nil
}

// Extract username from cookie with JWT verification
func (g *Gateway) extractUser(r *http.Request) (string, error) {
	// Try to get token from cookie first
	cookie, err := r.Cookie("auth_token")
	var tokenString string
	
	if err != nil {
		// Fallback to Authorization header for backward compatibility
		auth := r.Header.Get("Authorization")
		if auth == "" {
			return "", fmt.Errorf("missing auth_token cookie and Authorization header")
		}
		
		if !strings.HasPrefix(auth, "Bearer ") {
			return "", fmt.Errorf("invalid Authorization format, expected 'Bearer <token>'")
		}
		
		tokenString = strings.TrimPrefix(auth, "Bearer ")
		log.Printf("Using token from Authorization header (fallback)")
	} else {
		tokenString = cookie.Value
		log.Printf("Using token from cookie")
	}
	
	// Verify JWT token
	claims, err := g.verifyToken(tokenString)
	if err != nil {
		log.Printf("JWT verification failed: %v", err)
		return "", fmt.Errorf("invalid JWT token: %w", err)
	}
	
	// Extract username from claims
	username, ok := claims["username"].(string)
	if !ok {
		log.Printf("Username not found in JWT claims")
		return "", fmt.Errorf("username not found in JWT claims")
	}
	
	log.Printf("JWT verified successfully for user: %s", username)
	return username, nil
}

// Check if user has permission to access the path
func (g *Gateway) checkACL(username, path, method string) (bool, string) {
	user, exists := g.users[username]
	if !exists {
		return false, "User not found"
	}

	// Special check: path ends with 'admin'
	if strings.HasSuffix(path, "admin") && username != "admin" {
		return false, "Only 'admin' user can access paths ending with 'admin'"
	}

	// Find matching ACL rule (exact match)
	var matchedRule *ACLRule
	for _, rule := range g.aclRules {
		if path == rule.Path {
			matchedRule = &rule
			break
		}
	}

	// If no rule found and user is guest, allow access
	if matchedRule == nil {
		if username == "guest"  {
			log.Printf("No ACL rule found, but user is guest - allowing access")
			return true, "Guest access granted (no specific rule)"
		}
		return false, "No ACL rule found for path"
	}

	// Check method
	methodAllowed := false
	for _, allowedMethod := range matchedRule.Methods {
		if allowedMethod == method {
			methodAllowed = true
			break
		}
	}
	if !methodAllowed {
		return false, fmt.Sprintf("Method %s not allowed", method)
	}

	// Check role
	if !g.userHasRoleOrHigher(user.Roles, matchedRule.RequiredRole) {
		return false, fmt.Sprintf("User %s does not have required role: %s", username, matchedRule.RequiredRole)
	}

	return true, "Access granted"
}

// Check if user has the required role or higher
func (g *Gateway) userHasRoleOrHigher(userRoles []string, requiredRole string) bool {
	roleLevel := map[string]int{"guest": 1, "user": 2, "admin": 3}
	
	requiredLevel, ok := roleLevel[requiredRole]
	if !ok {
		return false
	}
	
	for _, userRole := range userRoles {
		if level, ok := roleLevel[userRole]; ok && level >= requiredLevel {
			return true
		}
	}
	return false
}

// Health check handler
func (g *Gateway) healthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"status":    "healthy",
		"service":   "gateway",
		"timestamp": time.Now().Format(time.RFC3339),
		"users_count": len(g.users),
		"acl_rules_count": len(g.aclRules),
	})
}

// Login handler to generate JWT token
func (g *Gateway) loginHandler(w http.ResponseWriter, r *http.Request) {
	// Serve HTML page for GET requests
	if r.Method == http.MethodGet {
		g.serveLoginPage(w, r)
		return
	}
	
	w.Header().Set("Content-Type", "application/json")
	
	// Only accept POST for authentication
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Method not allowed, use POST",
		})
		return
	}
	
	// Parse request body with size limit
	r.Body = http.MaxBytesReader(w, r.Body, 4096) // Max 4KB for form data
	
	var loginReq struct {
		Username string `json:"username"`
		Password string `json:"password"`
		Type     string `json:"type"`
		Token    string `json:"token"`
	}
	
	if err := json.NewDecoder(r.Body).Decode(&loginReq); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Invalid JSON body",
		})
		return
	}
	
	// Validate input length to prevent DoS
	if len(loginReq.Username) > 100 {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Username too long",
		})
		return
	}
	
	// Call backend /login endpoint for authentication
	backendURL := os.Getenv("BACKEND_URL")
	if backendURL == "" {
		backendURL = "http://backend:5000"
	}
	
	// Build form data for backend
	formData := url.Values{}
	formData.Set("username", loginReq.Username)
	if loginReq.Password != "" {
		formData.Set("password", loginReq.Password)
		formData.Set("type", "password")
	}
	if loginReq.Token != "" {
		formData.Set("token", loginReq.Token)
		if loginReq.Type != "" {
			formData.Set("type", loginReq.Type)
		}
	}
	
	// Make request to backend
	backendReq, err := http.NewRequest("POST", backendURL+"/login", strings.NewReader(formData.Encode()))
	if err != nil {
		log.Printf("Failed to create backend request: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Authentication service error",
		})
		return
	}
	backendReq.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	
	client := &http.Client{Timeout: 10 * time.Second}
	backendResp, err := client.Do(backendReq)
	if err != nil {
		log.Printf("Backend authentication error: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Authentication service unavailable",
		})
		return
	}
	defer backendResp.Body.Close()
	
	// Check backend response
	if backendResp.StatusCode != http.StatusOK {
		log.Printf("Login failed for user: %s (backend returned %d)", loginReq.Username, backendResp.StatusCode)
		w.WriteHeader(backendResp.StatusCode)
		io.Copy(w, backendResp.Body)
		return
	}
	
	// Parse backend response
	var backendResult struct {
		Username string `json:"username"`
		Role     string `json:"role"`
	}
	if err := json.NewDecoder(backendResp.Body).Decode(&backendResult); err != nil {
		log.Printf("Failed to parse backend response: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Authentication service error",
		})
		return
	}

	var username string
	var userRoles []string
	if backendResult.Role == "admin" {
		username = "admin"
		userRoles = []string{"admin", "user", "guest"}
	} else {
		username = "guest"
		userRoles = []string{"guest"}
	}

	g.users[username] = User{
		Username: username,
		Roles:    userRoles,
	}

	secret := g.jwtSecret
	
	// Create JWT token
	now := time.Now()
	claims := jwt.MapClaims{
		"sub":      username,
		"username": username,
		"roles":    userRoles,
		"iat":      now.Unix(),
		"exp":      now.Add(1 * time.Hour).Unix(), // Expires in 1 hour
		"iss":      "gateway",                      // Issuer
	}
	
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(secret))
	if err != nil {
		log.Printf("Failed to sign token: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"error": "Failed to generate token",
		})
		return
	}
	
	log.Printf("Login successful for user: %s (role: %s)", username, backendResult.Role)
	
	// Set HTTP-only cookie for security
	cookie := &http.Cookie{
		Name:     "auth_token",
		Value:    tokenString,
		Path:     "/",
		HttpOnly: true,
		Secure:   false, // Set to true in production with HTTPS
		SameSite: http.SameSiteLaxMode,
		Expires:  now.Add(1 * time.Hour),
	}
	http.SetCookie(w, cookie)
	
	// Return success response (without token in body for security)
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success":  true,
		"username": username,
		"roles":    userRoles,
		"expires":  now.Add(1 * time.Hour).Format(time.RFC3339),
		"message":  "Authentication successful, token set in cookie",
	})
}

// Serve login page HTML
func (g *Gateway) serveLoginPage(w http.ResponseWriter, r *http.Request) {
	// Try to read the login.html file
	htmlPath := filepath.Join("static", "login.html")
	htmlContent, err := os.ReadFile(htmlPath)
	if err != nil {
		log.Printf("Error reading login page: %v", err)
		http.Error(w, "Login page not found", http.StatusInternalServerError)
		return
	}
	
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	w.Write(htmlContent)
}

// Serve dashboard page HTML
func (g *Gateway) serveDashboardPage(w http.ResponseWriter, r *http.Request) {
	// Try to read the dashboard.html file
	htmlPath := filepath.Join("static", "dashboard.html")
	htmlContent, err := os.ReadFile(htmlPath)
	if err != nil {
		log.Printf("Error reading dashboard page: %v", err)
		http.Error(w, "Dashboard page not found", http.StatusInternalServerError)
		return
	}
	
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	w.Write(htmlContent)
}

// Logout handler to clear authentication cookie
func (g *Gateway) logoutHandler(w http.ResponseWriter, r *http.Request) {
	// Clear the authentication cookie
	cookie := &http.Cookie{
		Name:     "auth_token",
		Value:    "",
		Path:     "/",
		HttpOnly: true,
		Secure:   false, // Set to true in production with HTTPS
		SameSite: http.SameSiteLaxMode,
		Expires:  time.Unix(0, 0), // Set to past date to delete cookie
		MaxAge:   -1,
	}
	http.SetCookie(w, cookie)
	
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Logged out successfully",
	})
}

// Generate random JWT secret
func generateRandomSecret() string {
	bytes := make([]byte, 32) // 256 bits
	if _, err := rand.Read(bytes); err != nil {
		log.Fatalf("Failed to generate random secret: %v", err)
	}
	return base64.StdEncoding.EncodeToString(bytes)
}

func main() {
	// Get or generate JWT secret
	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		jwtSecret = generateRandomSecret()
		log.Println("JWT_SECRET not set in environment, generated random secret for this session")
		log.Printf("Generated secret (base64): %s", jwtSecret)
	} else {
		log.Println("Using JWT_SECRET from environment")
	}
	
	// Validate secret strength
	if len(jwtSecret) < 32 {
		log.Println("WARNING: JWT_SECRET is too short, should be at least 32 characters")
	}

	// Create gateway instance with JWT secret
	gateway := &Gateway{
		jwtSecret: jwtSecret,
	}

	// Initialize data
	gateway.initializeData()

	// Setup routes
	http.HandleFunc("/gateway-health", gateway.healthHandler)
	http.HandleFunc("/gateway-login", gateway.loginHandler)
	http.HandleFunc("/gateway-logout", gateway.logoutHandler)
	http.HandleFunc("/gateway-dashboard", gateway.serveDashboardPage)
	http.HandleFunc("/", gateway.handler)

	// Start server
	port := os.Getenv("PORT")
	if port == "" {
		port = "8000"
	}

	// Get backend URL for logging
	backendURL := os.Getenv("BACKEND_URL")
	if backendURL == "" {
		backendURL = "http://backend:5000"
	}

	log.Printf("Gateway starting on port %s", port)
	log.Printf("Backend URL: %s", backendURL)
	log.Printf("Loaded %d users and %d ACL rules", len(gateway.users), len(gateway.aclRules))

	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
