FROM ghcr.io/puppeteer/puppeteer:latest

WORKDIR /app

USER root

COPY package*.json ./

RUN npm install

COPY . .

EXPOSE 3000
CMD ["node","server.js"]
