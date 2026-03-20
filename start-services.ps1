# LMS System Startup Script

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "LMS System Startup Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Start Backend Service
Write-Host "[1/2] Starting Backend Service..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\31074\Desktop\LMS-project\lms-backend'; Write-Host 'Backend Service Log:' -ForegroundColor Green; mvn spring-boot:run"
Start-Sleep -Seconds 5

# Start Frontend Service
Write-Host "[2/2] Starting Frontend Service..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\31074\Desktop\LMS-project\LMS-SYSTEM-master'; Write-Host 'Frontend Service Log:' -ForegroundColor Green; npm run serve"
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "Services Started!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Backend:  http://localhost:8099" -ForegroundColor Cyan
Write-Host "Swagger:  http://localhost:8099/swagger-ui.html" -ForegroundColor Cyan
Write-Host "Frontend: http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Check the new PowerShell windows for service logs" -ForegroundColor Yellow
