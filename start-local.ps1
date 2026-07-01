# =====================================================================
# HotelChain Pro - Local Startup Script (chạy từ PowerShell)
# Cách dùng: .\start-local.ps1
#
# Kiến trúc:
#   - Docker containers (PG, Redis, RabbitMQ, MinIO) chạy trong WSL
#   - Spring Boot cũng chạy trong WSL (kết nối trực tiếp qua localhost)
#   - Windows truy cập app qua portproxy: localhost:8080 -> WSL:8080
# =====================================================================

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   HotelChain Pro - Local Dev Startup    " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# ── Bước 1: Start Docker containers trong WSL ───────────────────────
Write-Host "[1/3] Khởi động Docker containers trong WSL..." -ForegroundColor Yellow

$containerStatus = wsl -d Ubuntu-22.04 -u root docker ps --format "{{.Names}}" 2>$null
$needed = @("pg-hotelchain","redis-hotelchain","rabbitmq-hotelchain","minio-hotelchain")
$missing = $needed | Where-Object { $containerStatus -notcontains $_ }

if ($missing.Count -gt 0) {
    Write-Host "  Đang start: $($missing -join ', ')" -ForegroundColor Gray
    wsl -d Ubuntu-22.04 -u root docker start pg-hotelchain redis-hotelchain rabbitmq-hotelchain minio-hotelchain 2>$null | Out-Null
    Start-Sleep -Seconds 4
}

# Kiểm tra PostgreSQL ready
$pgReady = wsl -d Ubuntu-22.04 -u root bash -c "docker exec pg-hotelchain pg_isready -U hotelchain 2>&1"
if ($pgReady -like "*accepting connections*") {
    Write-Host "  ✅ PostgreSQL  :5432  - Ready" -ForegroundColor Green
} else {
    Write-Host "  ⏳ PostgreSQL đang khởi tạo, đợi thêm 5 giây..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}
Write-Host "  ✅ Redis       :6379  - Running" -ForegroundColor Green
Write-Host "  ✅ RabbitMQ    :5672  - Running" -ForegroundColor Green
Write-Host "  ✅ MinIO       :9000  - Running" -ForegroundColor Green

# ── Bước 2: Lấy WSL IP để hiển thị URL ─────────────────────────────
$wslIp = (wsl -d Ubuntu-22.04 hostname -I 2>$null).Trim().Split(" ")[0]

Write-Host ""
Write-Host "[2/3] Thông tin truy cập:" -ForegroundColor Yellow
Write-Host "  🌐 Frontend / API    -> http://localhost:8080" -ForegroundColor Cyan
Write-Host "  📄 Swagger UI        -> http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host "  🐰 RabbitMQ UI       -> http://${wslIp}:15672  (guest/guest)" -ForegroundColor Cyan
Write-Host "  🗄️  MinIO Console     -> http://${wslIp}:9001   (minioadmin/minioadmin)" -ForegroundColor Cyan
Write-Host ""

# ── Bước 3: Chạy Spring Boot trong WSL ──────────────────────────────
Write-Host "[3/3] Khởi động Spring Boot (bên trong WSL)..." -ForegroundColor Yellow
Write-Host "  ⏳ Đợi ~30-60 giây để ứng dụng compile và start" -ForegroundColor Gray
Write-Host "  ↩  Nhấn Ctrl+C để dừng" -ForegroundColor Gray
Write-Host ""

wsl -d Ubuntu-22.04 -u root bash -c "cd /mnt/d/web2 && mvn spring-boot:run 2>&1"
