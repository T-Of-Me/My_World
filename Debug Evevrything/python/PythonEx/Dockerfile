FROM python:3.11-slim

WORKDIR /app

# Cài framework tối thiểu
RUN pip install --no-cache-dir flask

# Không COPY code vào image — code sẽ được mount qua volume từ ./src
ENV PYTHONUNBUFFERED=1
