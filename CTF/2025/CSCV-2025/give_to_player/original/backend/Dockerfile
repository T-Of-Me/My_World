
FROM python:3.9-slim

# Setup flag 
ADD readflag /
RUN chown 0:1337 /readflag && \
    chmod 2555 /readflag

# Install curl for health checks
RUN apt-get update && apt-get install -y curl pkg-config default-libmysqlclient-dev build-essential && rm -rf /var/lib/apt/lists/*

# Create app user with limited privileges
RUN groupadd -r app-user && useradd -r -g app-user app-user

# Create webroot directory and set permissions
WORKDIR /app
RUN pip install flask requests pycryptodome

# Copy application files
COPY *.py ./
COPY *.so ./
COPY templates/ ./templates/

# Create uploads directory and database directory with proper permissions
RUN mkdir -p uploads data && \
    chown -R root:root /app && \
    chown -R app-user:app-user /app/templates/ /app/uploads/ /app/data/ && \
    chmod -R 644 /app && \
    chmod 755 /app && \
    chmod +x *.py && \
    chmod 755 /app/templates/ /app/uploads/ /app/data/

# Switch to non-root user
USER app-user

EXPOSE 5000
CMD ["python", "app.py"]

