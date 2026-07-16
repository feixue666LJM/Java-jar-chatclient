<#
.SYNOPSIS
    Build single hybrid JAR: Fabric 1.20.1 mod + standalone app
#>
param(
    [string]$SourceDir = ".",
    [string]$OutputDir = "build",
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

# === Resolve paths ===
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not [System.IO.Path]::IsPathRooted($SourceDir)) {
    $SourceDir = Join-Path $ScriptRoot $SourceDir
}
if (-not [System.IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir = Join-Path $ScriptRoot $OutputDir
}
$SourceDir = (Resolve-Path $SourceDir).Path
$StubDir = Join-Path $SourceDir "stubs"
$StubClassesDir = Join-Path $ScriptRoot "stubs-classes"
$ClassDir = Join-Path $ScriptRoot "classes"
$OutputDir = (New-Item -ItemType Directory -Force -Path $OutputDir).FullName

if ($Clean) {
    Remove-Item -Recurse -Force $StubClassesDir, $ClassDir -ErrorAction SilentlyContinue
}

# ============================================
# Step 1: Compile stubs (Fabric API placeholders)
# ============================================
Write-Host "=== Step 1: Compile stubs ===" -ForegroundColor Cyan
Remove-Item -Recurse -Force $StubClassesDir -ErrorAction SilentlyContinue
$null = New-Item -ItemType Directory -Force -Path $StubClassesDir
$stubFiles = Get-ChildItem -Path $StubDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if ($stubFiles.Count -gt 0) {
    & javac -d $StubClassesDir -encoding UTF8 $stubFiles
    if ($LASTEXITCODE -ne 0) { Write-Host "Stub compilation failed!" -ForegroundColor Red; exit 1 }
    Write-Host "Stubs compiled: $($stubFiles.Count) files" -ForegroundColor Green
}

# ============================================
# Step 2: Compile all source code
# ============================================
Write-Host "=== Step 2: Compile ALL source ===" -ForegroundColor Cyan
Remove-Item -Recurse -Force $ClassDir -ErrorAction SilentlyContinue
$null = New-Item -ItemType Directory -Force -Path $ClassDir
$allJavaFiles = Get-ChildItem -Path $SourceDir -Recurse -Filter "*.java" |
    Where-Object { $_.FullName -notmatch '\\stubs\\' } |
    ForEach-Object { $_.FullName }

Write-Host "Compiling $($allJavaFiles.Count) source files (stubs on classpath)" -ForegroundColor Yellow
& javac -d $ClassDir -encoding UTF8 -cp $StubClassesDir $allJavaFiles
if ($LASTEXITCODE -ne 0) { Write-Host "Compilation failed!" -ForegroundColor Red; exit 1 }
Write-Host "All classes compiled" -ForegroundColor Green

# ============================================
# Step 3: Package single hybrid JAR
# ============================================
Write-Host "=== Step 3: Package JAR ===" -ForegroundColor Cyan

# Copy fabric.mod.json to class output root
Copy-Item "$SourceDir\fabric.mod.json" -Destination "$ClassDir\fabric.mod.json"

# Copy META-INF/MANIFEST.MF (for standalone java -jar)
$metaDir = "$ClassDir\META-INF"
$null = New-Item -ItemType Directory -Force -Path $metaDir
Copy-Item "$SourceDir\META-INF\MANIFEST.MF" -Destination "$metaDir\MANIFEST.MF" -Force

# Build list of files (exclude stub .class files)
Push-Location $ClassDir
$allOutputFiles = Get-ChildItem -Path "." -Recurse -File |
    Where-Object { $_.FullName -notmatch '\\net\\' }
$paths = @()
foreach ($f in $allOutputFiles) {
    $rel = $f.FullName.Substring((Get-Location).Path.Length + 1) -replace '\\', '/'
    $paths += $rel
}

# Build JAR: cfm = create, manifest, jar-file → manifest must come right after cfm
$jarPath = "$OutputDir\ChatClient-mod-1.0.0.jar"
$manifestFile = Resolve-Path "META-INF/MANIFEST.MF" | Select-Object -ExpandProperty Path
& jar cfm $jarPath $manifestFile $paths
Pop-Location

if ($LASTEXITCODE -eq 0 -and (Test-Path $jarPath)) {
    Write-Host "Hybrid JAR: $jarPath" -ForegroundColor Green
    Write-Host "Size: $((Get-Item $jarPath).Length / 1KB) KB" -ForegroundColor Yellow
} else {
    Write-Host "JAR packaging failed!" -ForegroundColor Red
    exit 1
}

# ============================================
# Step 4: Cleanup
# ============================================
Remove-Item -Recurse -Force $StubClassesDir, $ClassDir -ErrorAction SilentlyContinue

Write-Host "`n=== Build complete ===" -ForegroundColor Green
Write-Host "Single JAR working as both:" -ForegroundColor White
Write-Host "  1. Fabric 1.20.1 mod    ->  auto-login with Minecraft ID" -ForegroundColor Gray
Write-Host "  2. Standalone app       ->  double-click, login window" -ForegroundColor Gray
