FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py /app/
COPY templates /app/templates
COPY static /app/static
COPY gunicorn.conf.py .
COPY flag.txt .  

RUN set -eux; \
    RAND=$(cat /dev/urandom | tr -dc 'a-z0-9' | head -c6); \
    FLAG_NAME="flag_${RAND}"; \
    cp flag.txt /${FLAG_NAME}; \
    rm -f flag.txt; 

RUN groupadd -r appuser && useradd -r -g appuser appuser

RUN mkdir -p /app/uploads \
    && chown -R appuser:appuser /app/uploads \
    && chmod 755 /app/uploads

RUN chown -R root:root /app \
    && chown -R appuser:appuser /app/uploads
    
USER appuser

EXPOSE 8088

CMD ["gunicorn", "--config", "gunicorn.conf.py", "app:app"]