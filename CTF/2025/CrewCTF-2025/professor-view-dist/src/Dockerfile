FROM node:22-slim
USER root
RUN apt update \
    && apt install -y tini xvfb chromium mesa-utils libgl1-mesa-dri libxi6 libgconf-2-4\
    && apt clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

WORKDIR /app
COPY package.json /app/
COPY bot.js server.js /app/
COPY static /app/static/
RUN npm install
RUN chmod -R 555 /app/

USER node
EXPOSE 8000
ENTRYPOINT ["/bin/tini", "--"]
CMD xvfb-run --server-args="-screen 0 1280x1024x24" node server.js
