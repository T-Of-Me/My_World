FROM node:20-alpine
WORKDIR /usr/src/app
COPY package*.json ./
# Install deps (production by default); dev uses docker-compose to run npm install again if needed
RUN npm ci || npm install
COPY . .
ENV PORT=3000
EXPOSE 3000
CMD ["node", "src/app.js"]
