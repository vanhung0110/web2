$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8888/api"

function Invoke-Api {
    param (
        [string]$Method,
        [string]$Endpoint,
        [hashtable]$Body = $null,
        [string]$Token = $null,
        [string]$UserId = $null
    )
    
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    if ($UserId) { $headers["X-User-Id"] = $UserId }

    $params = @{
        Uri = "$baseUrl$Endpoint"
        Method = $Method
        Headers = $headers
    }
    
    if ($Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $res = Invoke-RestMethod @params
        return $res
    } catch {
        $streamReader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $errResp = $streamReader.ReadToEnd()
        Write-Error "API CALL FAILED: $Method $Endpoint `nResponse: $errResp"
        return $null
    }
}

Write-Host "--- BẮT ĐẦU TEST TOÀN DIỆN ---"

# 1. Login Admin
Write-Host "1. Test Đăng nhập Admin..."
$adminLogin = Invoke-Api -Method POST -Endpoint "/auth/login" -Body @{ phone = "0962750432" }
$adminToken = $adminLogin.data.token
$adminId = $adminLogin.data.userId
Write-Host "Admin Login Success! Token length: $($adminToken.Length)"

# 2. Add Branch
Write-Host "2. Test Thêm Cơ sở..."
$branch = Invoke-Api -Method POST -Endpoint "/branches" -Token $adminToken -Body @{ name = "Test Branch"; address = "123 Test St" }
$branchId = $branch.data.id
Write-Host "Branch Created! ID: $branchId"

# 3. Add Room
Write-Host "3. Test Thêm Phòng..."
$room = Invoke-Api -Method POST -Endpoint "/rooms" -Token $adminToken -Body @{ branchId = $branchId; roomNumber = "P101"; floor = 1; monthlyRent = 2000000 }
$roomId = $room.data.id
Write-Host "Room Created! ID: $roomId"

# 4. Add Tenant
Write-Host "4. Test Thêm Người thuê (và tự động tạo User)..."
$tenant = Invoke-Api -Method POST -Endpoint "/tenants" -Token $adminToken -Body @{ roomId = $roomId; fullName = "Khach Test"; phone = "0999999999" }
$tenantId = $tenant.data.id
Write-Host "Tenant Created! ID: $tenantId"

# 5. Login User
Write-Host "5. Test Đăng nhập Khách thuê (vừa tạo)..."
$userLogin = Invoke-Api -Method POST -Endpoint "/auth/login" -Body @{ phone = "0999999999" }
$userToken = $userLogin.data.token
$userId = $userLogin.data.userId
Write-Host "User Login Success! Token length: $($userToken.Length)"

# 6. Submit Report (User)
Write-Host "6. Test Khách báo cáo điện nước..."
$report = Invoke-Api -Method POST -Endpoint "/reports" -Token $userToken -UserId $userId -Body @{ 
    reportMonth = 7; 
    reportYear = 2026; 
    waterOld = 10; waterNew = 15; 
    electricOld = 100; electricNew = 150; 
    note = "Test report" 
}
$reportId = $report.data.id
Write-Host "Report Submitted! ID: $reportId"

# 7. Approve Report (Admin)
Write-Host "7. Test Admin Duyệt báo cáo..."
$approve = Invoke-Api -Method PUT -Endpoint "/reports/$reportId/approve" -Token $adminToken -UserId $adminId
Write-Host "Report Approved! Total Cost: $($approve.data.totalCost)"

# 8. Mark Paid (Admin)
Write-Host "8. Test Admin Xác nhận thu tiền..."
$paid = Invoke-Api -Method PUT -Endpoint "/reports/$reportId/pay" -Token $adminToken -UserId $adminId
Write-Host "Report Paid! isPaid: $($paid.data.isPaid)"

Write-Host "--- KẾT THÚC TEST ---"
