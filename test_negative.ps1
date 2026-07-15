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

# Try to get the existing room b15ec067-e243-41e9-89f6-ab3e819c48c2 which has a tenant (or create new)
$rooms = Invoke-Api -Method GET -Endpoint "/rooms" -Token $adminToken
$roomId = $rooms.data[0].id

Write-Host "Test 1: Delete an occupied room"
Invoke-Api -Method DELETE -Endpoint "/rooms/$roomId" -Token $adminToken

Write-Host "Test 2: Create user and report negative usage"
$userLogin = Invoke-Api -Method POST -Endpoint "/auth/login" -Body @{ phone = "0999999999" }
$userToken = $userLogin.data.token
$userId = $userLogin.data.userId

Invoke-Api -Method POST -Endpoint "/reports" -Token $userToken -UserId $userId -Body @{ 
    reportMonth = 8; 
    reportYear = 2026; 
    waterOld = 100; waterNew = 50; 
    electricOld = 100; electricNew = 50; 
    note = "Negative test" 
}
