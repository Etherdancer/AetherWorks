# build-release.ps1 — Build release AAB with automatic Git versioning
# Usage: .\build-release.ps1
# Usage with custom message: .\build-release.ps1 -Message "fix: resolved crash on startup"

param(
    [string]$Message = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Extract version info from build.gradle.kts
$gradleFile = "app\build.gradle.kts"
$content = Get-Content $gradleFile -Raw
$versionCode = if ($content -match 'versionCode\s*=\s*(\d+)') { $Matches[1] } else { "unknown" }
$versionName = if ($content -match 'versionName\s*=\s*"([^"]+)"') { $Matches[1] } else { "unknown" }

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ClearSpace Release Builder" -ForegroundColor Cyan
Write-Host "  Version: $versionName (code: $versionCode)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Stage all changes
Write-Host "[1/3] Committing changes to Git..." -ForegroundColor Yellow
git add -A

# Check if there are changes to commit
$status = git status --porcelain
if ($status) {
    if ($Message -eq "") {
        $Message = "release: v$versionName (versionCode $versionCode)"
    }
    git commit -m $Message
    Write-Host "  Committed: $Message" -ForegroundColor Green
} else {
    Write-Host "  No changes to commit, skipping." -ForegroundColor DarkGray
}

# Build the AAB
Write-Host ""
Write-Host "[2/3] Building release AAB..." -ForegroundColor Yellow
.\gradlew.bat bundleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "BUILD FAILED!" -ForegroundColor Red
    exit 1
}

# Tag the release
Write-Host ""
Write-Host "[3/3] Tagging release..." -ForegroundColor Yellow
$tag = "v$versionName-$versionCode"
$existingTag = git tag -l $tag
if (-not $existingTag) {
    git tag -a $tag -m "Release $versionName (versionCode $versionCode)"
    Write-Host "  Tagged: $tag" -ForegroundColor Green
} else {
    Write-Host "  Tag $tag already exists, skipping." -ForegroundColor DarkGray
}

# Output result
$aabPath = "app\build\outputs\bundle\release\app-release.aab"
$aabSize = [math]::Round((Get-Item $aabPath).Length / 1MB, 1)

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host "  AAB: $aabPath ($aabSize MB)" -ForegroundColor Green
Write-Host "  Version: $versionName (code: $versionCode)" -ForegroundColor Green
Write-Host "  Git tag: $tag" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
