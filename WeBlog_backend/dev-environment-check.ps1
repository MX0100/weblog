# ======================================
# WeBlog 开发环境检查脚本 (PowerShell)
# ======================================

param(
    [switch]$IncludePgAdmin = $false,
    [switch]$Detailed = $false
)

Write-Host "🔍 开始检查WeBlog开发环境..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

# 检查结果统计
$script:TotalChecks = 0
$script:PassedChecks = 0
$script:FailedChecks = 0

# 检查函数
function Test-Service {
    param(
        [string]$ServiceName,
        [scriptblock]$CheckCommand,
        [string]$SuccessMessage = "正常",
        [string]$FailureMessage = "异常"
    )
    
    $script:TotalChecks++
    Write-Host "`n🔍 检查 $ServiceName..." -ForegroundColor Blue
    
    try {
        $result = & $CheckCommand
        if ($LASTEXITCODE -eq 0 -or $result) {
            Write-Host "✅ $ServiceName $SuccessMessage" -ForegroundColor Green
            $script:PassedChecks++
            return $true
        } else {
            Write-Host "❌ $ServiceName $FailureMessage" -ForegroundColor Red
            $script:FailedChecks++
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName $FailureMessage" -ForegroundColor Red
        if ($Detailed) {
            Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        $script:FailedChecks++
        return $false
    }
}

function Test-HttpService {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$ExpectedStatus = 200
    )
    
    $script:TotalChecks++
    Write-Host "`n🔍 检查 $ServiceName ($Url)..." -ForegroundColor Blue
    
    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "✅ $ServiceName 正常 (状态码: $($response.StatusCode))" -ForegroundColor Green
            $script:PassedChecks++
            return $true
        } else {
            Write-Host "❌ $ServiceName 异常 (状态码: $($response.StatusCode), 期望: $ExpectedStatus)" -ForegroundColor Red
            $script:FailedChecks++
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName 连接失败" -ForegroundColor Red
        if ($Detailed) {
            Write-Host "   错误: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        $script:FailedChecks++
        return $false
    }
}

Write-Host "========================================" -ForegroundColor Gray
Write-Host "🐳 Docker 服务检查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

# 1. 检查Docker
Test-Service "Docker 守护进程" { docker info 2>$null }

# 2. 检查Docker Compose
Test-Service "Docker Compose" { docker-compose --version 2>$null }

# 3. 显示容器状态
Write-Host "`n📦 当前容器状态:" -ForegroundColor Blue
try {
    docker-compose -f compose.yaml -f docker-compose.dev.yml ps
} catch {
    Write-Host "无法获取容器状态" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Gray
Write-Host "🗄️ 数据库服务检查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

# 4. 检查PostgreSQL容器
Test-Service "PostgreSQL 容器" { 
    docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres pg_isready -U weblog 2>$null 
}

# 5. 检查数据库连接
Test-Service "数据库连接" { 
    docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres psql -U weblog -d weblog -c "SELECT 1;" 2>$null 
}

# 6. 检查数据库表
$script:TotalChecks++
Write-Host "`n🔍 检查数据库表结构..." -ForegroundColor Blue
try {
    $tables = docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres psql -U weblog -d weblog -t -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public';" 2>$null
    $tableCount = ($tables | Where-Object { $_.Trim() -ne "" }).Count
    
    if ($tableCount -gt 0) {
        Write-Host "✅ 数据库表存在 ($tableCount 个表)" -ForegroundColor Green
        $script:PassedChecks++
        
        if ($Detailed) {
            Write-Host "📋 数据库表列表:" -ForegroundColor Yellow
            docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T postgres psql -U weblog -d weblog -c "\dt"
        }
    } else {
        Write-Host "❌ 没有发现数据库表" -ForegroundColor Red
        $script:FailedChecks++
    }
} catch {
    Write-Host "❌ 无法检查数据库表" -ForegroundColor Red
    $script:FailedChecks++
}

Write-Host "`n========================================" -ForegroundColor Gray
Write-Host "☕ 后端服务检查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

# 7. 检查后端容器状态
Test-Service "后端容器" { 
    $containers = docker-compose -f compose.yaml -f docker-compose.dev.yml ps weblog-app 2>$null
    return $containers -match "Up"
}

# 8. 检查后端健康检查
Test-HttpService "后端健康检查" "http://localhost:8080/actuator/health" 200

# 9. 检查后端API根路径
Test-HttpService "后端API根路径" "http://localhost:8080/" 200

# 10. 检查Actuator端点
$script:TotalChecks++
Write-Host "`n🔍 检查Spring Boot Actuator端点..." -ForegroundColor Blue
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator" -TimeoutSec 10
    $endpointCount = $response._links.PSObject.Properties.Count
    if ($endpointCount -gt 0) {
        Write-Host "✅ Actuator端点正常 ($endpointCount 个端点)" -ForegroundColor Green
        $script:PassedChecks++
    } else {
        Write-Host "❌ Actuator端点异常" -ForegroundColor Red
        $script:FailedChecks++
    }
} catch {
    Write-Host "❌ 无法访问Actuator端点" -ForegroundColor Red
    $script:FailedChecks++
}

Write-Host "`n========================================" -ForegroundColor Gray
Write-Host "🌐 前端服务检查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

# 11. 检查Node.js
Test-Service "Node.js" { node --version 2>$null }

# 12. 检查npm
Test-Service "npm" { npm --version 2>$null }

# 13. 检查前端依赖
$script:TotalChecks++
Write-Host "`n🔍 检查前端依赖..." -ForegroundColor Blue
$frontendPath = "..\WeBlog-frontend"
if ((Test-Path "$frontendPath\node_modules") -and (Test-Path "$frontendPath\package-lock.json")) {
    Write-Host "✅ 前端依赖已安装" -ForegroundColor Green
    $script:PassedChecks++
} else {
    Write-Host "❌ 前端依赖未安装" -ForegroundColor Red
    Write-Host "💡 请运行: cd WeBlog-frontend && npm install" -ForegroundColor Yellow
    $script:FailedChecks++
}

# PgAdmin检查（可选）
if ($IncludePgAdmin) {
    Write-Host "`n========================================" -ForegroundColor Gray
    Write-Host "🔧 PgAdmin4 检查" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Gray
    
    # 14. 检查PgAdmin4
    $script:TotalChecks++
    Write-Host "`n🔍 检查PgAdmin4容器状态..." -ForegroundColor Blue
    try {
        $pgadminStatus = docker-compose -f compose.yaml -f docker-compose.dev.yml --profile tools ps pgadmin 2>$null
        if ($pgadminStatus -match "Up") {
            Write-Host "✅ PgAdmin4 容器运行中" -ForegroundColor Green
            $script:PassedChecks++
            Test-HttpService "PgAdmin4 Web界面" "http://localhost:5050" 200
        } else {
            Write-Host "⚠️ PgAdmin4 未启动 (需要使用 --profile tools)" -ForegroundColor Yellow
            Write-Host "💡 启动命令: docker-compose -f compose.yaml -f docker-compose.dev.yml --profile tools up -d pgadmin" -ForegroundColor Yellow
            $script:FailedChecks++
        }
    } catch {
        Write-Host "❌ 无法检查PgAdmin4状态" -ForegroundColor Red
        $script:FailedChecks++
    }
}

Write-Host "`n========================================" -ForegroundColor Gray
Write-Host "🔗 网络连通性检查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

# 15. 检查容器间网络
Test-Service "容器间网络连通性" { 
    docker-compose -f compose.yaml -f docker-compose.dev.yml exec -T weblog-app ping -c 1 postgres 2>$null 
}

Write-Host "`n========================================" -ForegroundColor Gray
Write-Host "📋 检查总结" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Gray

Write-Host "总检查项: $script:TotalChecks" -ForegroundColor Blue
Write-Host "通过: $script:PassedChecks" -ForegroundColor Green
Write-Host "失败: $script:FailedChecks" -ForegroundColor Red

if ($script:FailedChecks -eq 0) {
    Write-Host "`n🎉 所有检查通过！开发环境运行正常！" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️ 发现 $script:FailedChecks 个问题，请检查上述失败项" -ForegroundColor Red
    exit 1
} 