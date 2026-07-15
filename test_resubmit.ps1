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

$adminLogin = Invoke-Api -Method POST -Endpoint "/auth/login" -Body @{ phone = "0962750432" }
$adminToken = $adminLogin.data.token

$userLogin = Invoke-Api -Method POST -Endpoint "/auth/login" -Body @{ phone = "0999999999" }
$userToken = $userLogin.data.token
$userId = $userLogin.data.userId

Write-Host "Submitting report for month 9..."
$report = Invoke-Api -Method POST -Endpoint "/reports" -Token $userToken -UserId $userId -Body @{ 
    reportMonth = 9; 
    reportYear = 2026; 
    waterOld = 10; waterNew = 15; 
    electricOld = 100; electricNew = 150; 
    note = "Will be rejected" 
}
$reportId = $report.data.id

Write-Host "Admin rejecting report..."
Invoke-Api -Method PUT -Endpoint "/reports/$reportId/reject" -Token $adminToken -Body @{ rejectReason = "Please retake photo" }

Write-Host "User resubmitting for month 9..."
Invoke-Api -Method POST -Endpoint "/reports" -Token $userToken -UserId $userId -Body @{ 
    reportMonth = 9; 
    reportYear = 2026; 
    waterOld = 10; waterNew = 15; 
    electricOld = 100; electricNew = 150; 
    note = "Resubmission" 
}
