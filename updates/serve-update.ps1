param(
    [int]$VersionCode = 3,
    [string]$VersionName = "1.2.0",
    [string]$ApkPath = "..\app\build\outputs\apk\debug\app-debug.apk",
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$PublicDir = Join-Path $Root "public"
$ResolvedApk = Resolve-Path (Join-Path $Root $ApkPath)

New-Item -ItemType Directory -Force -Path $PublicDir | Out-Null
Copy-Item -Force -Path $ResolvedApk -Destination (Join-Path $PublicDir "app-debug.apk")

$manifest = [ordered]@{
    versionCode = $VersionCode
    versionName = $VersionName
    apkUrl = "app-debug.apk"
    notes = "New Skinwalker build is ready."
}

$manifest | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $PublicDir "skinwalker-update.json")

Write-Host "Update files are ready in $PublicDir"
Write-Host "Set Skinwalker update source to: http://<this-pc-ip>:$Port/skinwalker-update.json"
Write-Host "Local network IPs:"
Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } |
    Select-Object -ExpandProperty IPAddress

Push-Location $PublicDir
try {
    python -m http.server $Port
} finally {
    Pop-Location
}
