# Node.js 18 Dockerfile
FROM node:18.20.4-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Copy application code
COPY . .

# Create non-root user
RUN addgroup -g 1001 -S nodejs
RUN adduser -S ctfuser -u 1001
RUN chown -R ctfuser:nodejs /app
USER ctfuser

EXPOSE 3000

CMD ["npm", "start"]
