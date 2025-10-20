FROM python:3.12-alpine

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY . /app/
RUN apk add --no-cache gcc musl-dev
RUN gcc -o /readflag readflag.c
RUN mv flag /flag && rm readflag.c
RUN chown root:root /flag
RUN chmod 400 /flag
RUN chown root:root /readflag
RUN chmod 4755 /readflag

RUN adduser -D ctf
USER ctf
RUN pip install --no-cache-dir -r requirements.txt

CMD ["python", "app.py"]