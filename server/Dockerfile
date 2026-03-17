FROM python:3.11-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y default-libmysqlclient-dev gcc pkg-config \
    && rm -rf /var/lib/apt/lists/*

# Install dependencies first (cached layer)
COPY serverdj/requirements.txt ./serverdj/
RUN pip install --no-cache-dir -r serverdj/requirements.txt

COPY serverdj/ ./serverdj/
