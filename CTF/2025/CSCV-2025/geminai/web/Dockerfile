FROM node:18.20.4-alpine

RUN apk add --no-cache tzdata

ENV TZ=Asia/Ho_Chi_Minh

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

# Build TailwindCSS - ensure output directory exists and build CSS
RUN mkdir -p ./public/css && \
    npx tailwindcss -i ./public/css/style.css -o ./public/css/tailwind.css --minify

EXPOSE 3000

CMD ["npm", "start"]

