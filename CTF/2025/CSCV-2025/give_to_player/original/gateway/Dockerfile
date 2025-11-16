FROM golang:1.25-alpine AS base

WORKDIR /app

# Install system dependencies first
RUN apk --no-cache add curl ca-certificates git

# Install air for hot reload (new repo path)
RUN go install github.com/air-verse/air@latest

# Copy go files first (for caching)
COPY go.mod go.sum* ./
RUN go mod download 2>/dev/null || true

# Copy source code
COPY . .

# Build production binary
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/gateway .

# Development stage with Air (for patching)
FROM base AS dev
ENV CGO_ENABLED=0
EXPOSE 8000
CMD ["air", "-c", ".air.toml"]

# Production stage (minimal size)
FROM alpine:latest AS prod

# Install curl for health checks
RUN apk --no-cache add curl ca-certificates

WORKDIR /app

# Copy the binary from base stage
COPY --from=base /app/gateway .

# Copy static files
COPY static/ ./static/

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/gateway-health || exit 1

EXPOSE 8000

CMD ["./gateway"]
