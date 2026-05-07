# Install Java 17 (via Homebrew)
brew install openjdk@17

# Install Maven
brew install maven

# Install Docker Desktop
# Download dari: https://www.docker.com/products/docker-desktop/

# Verify installations
java -version
mvn -version
docker --version
docker-compose --version

# Run aplikasi dengan H2 in-memory database
mvn spring-boot:run

# Atau dengan profile explicit
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Akses aplikasi
# Web: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console

# 1. Start PostgreSQL saja (recommended untuk development)
docker-compose up -d postgres

# 2. Run Spring Boot dari IDE atau terminal
mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgres

# Akses aplikasi
# Web: http://localhost:8080
# PostgreSQL: localhost:5432


# Build dan run semua container
docker-compose up --build

# Run di background
docker-compose up -d

# Lihat logs
docker-compose logs -f quran-web

# Stop semua
docker-compose down

# Stop dan hapus data
docker-compose down -v

# Lihat status containers
docker-compose ps

# Restart aplikasi saja
docker-compose restart quran-web

# Masuk ke PostgreSQL shell
docker exec -it quran-postgres psql -U quran_user -d qurandb

# Backup database
docker exec quran-postgres pg_dump -U quran_user qurandb > backup.sql

# Restore database
docker exec -i quran-postgres psql -U quran_user qurandb < backup.sql

# Lihat resource usage
docker stats

# Clean up unused images/volumes
docker system prune -a


# Start pgAdmin
docker-compose up -d pgadmin

# Akses: http://localhost:5050
# Email: admin@quran.local
# Password: admin


# Build JAR file
mvn clean package -DskipTests

# Build Docker image
docker build -t quran-web:latest .

# Run production image locally (test)
docker run -p 8080:8080 \
-e SPRING_PROFILES_ACTIVE=prod \
-e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/qurandb \
-e DATABASE_USERNAME=quran_user \
-e DATABASE_PASSWORD=quran_pass \
quran-web:latest


# 1. Build image dengan tag cloud registry
docker build -t asia-southeast1-docker.pkg.dev/PROJECT/quran/quran-web:v1.0 .

# 2. Push ke registry (contoh GCP)
docker push asia-southeast1-docker.pkg.dev/PROJECT/quran/quran-web:v1.0

# 3. Deploy (via cloud console atau CLI)
# Set environment variables:
#   - SPRING_PROFILES_ACTIVE=prod
#   - DATABASE_URL=jdbc:postgresql://CLOUD_DB_HOST:5432/qurandb
#   - DATABASE_USERNAME=xxx
#   - DATABASE_PASSWORD=xxx


# Run dengan DevTools
mvn spring-boot:run

# Setiap kali save file .java atau .html, aplikasi auto-restart
# Thymeleaf templates juga auto-reload


# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics (jika actuator enabled)
curl http://localhost:8080/actuator/metrics


# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=QuranServiceTest

# Run dengan coverage
mvn clean test jacoco:report

# Skip tests saat build
mvn clean package -DskipTests


# Required
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://your-db-host:5432/qurandb
export DATABASE_USERNAME=your_username
export DATABASE_PASSWORD=your_password

# Optional
export PORT=8080
export AUDIO_CDN_URL=https://your-cdn.com/audio

# Run
java -jar target/quran-web-0.0.1-SNAPSHOT.jar


docker-compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=dev-postgres

# Cari process yang pakai port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Restart PostgreSQL container
docker-compose restart postgres

# Check logs
docker-compose logs postgres

# Tambah memory di JAVA_OPTS
export JAVA_OPTS="-Xms512m -Xmx1024m"
mvn spring-boot:run