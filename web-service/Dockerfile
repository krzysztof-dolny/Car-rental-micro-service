# Użyj obrazu z Pythonem
FROM python:3.12-slim

WORKDIR /app

# Skopiuj pliki projektu do kontenera
COPY . .

# Zainstaluj wymagane biblioteki
RUN pip install --no-cache-dir -r requirements.txt

# Uruchom aplikację Flask
CMD ["python", "run.py"]
