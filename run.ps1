# Build & run the JavaFX poker-deck app.
# No Maven required: uses the local JDK plus the JavaFX jars under /lib.
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$libDir = Join-Path $root "lib"
$outDir = Join-Path $root "out"
$srcDir = Join-Path $root "src\main\java"
$resDir = Join-Path $root "src\main\resources"

# ---- 1. Prepare JavaFX runtime jars (reuse local .m2 cache first) ----
function Ensure-Lib {
    $need = @(
        "javafx-base-21.0.2-win.jar",
        "javafx-graphics-21.0.2-win.jar",
        "javafx-controls-21.0.2-win.jar"
    )
    $missing = $need | Where-Object { -not (Test-Path (Join-Path $libDir $_)) }
    if (-not $missing) { return }

    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    $m2 = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"
    foreach ($f in $missing) {
        $name = $f -replace '-21\.0\.2-win\.jar$', ''
        $src = Join-Path $m2 ($name + "\21.0.2\" + $f)
        if (Test-Path $src) {
            Copy-Item $src (Join-Path $libDir $f) -Force
            Write-Host ("[lib] copied " + $f)
        } else {
            Write-Host ("[lib] missing " + $f + " -- resolve deps with IDEA/Maven or place it in lib dir.") -ForegroundColor Red
            exit 1
        }
    }
}
Ensure-Lib

# ---- 2. Compile ----
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
Get-ChildItem $resDir -File -ErrorAction SilentlyContinue | Copy-Item -Destination $outDir -Force

$java = (Get-Command java -ErrorAction Stop).Source
$javac = Join-Path (Split-Path $java) "javac.exe"
if (-not (Test-Path $javac)) { $javac = "javac" }

$sources = @(Get-ChildItem $srcDir -Recurse -Filter *.java | ForEach-Object FullName)
$modulePath = $libDir

Write-Host "[build] compiling..."
& $javac --module-path $modulePath --add-modules javafx.controls -encoding UTF-8 -parameters -d $outDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# ---- 3. Run ----
Write-Host "[run] launching..."
& $java --enable-native-access=javafx.graphics --module-path $modulePath --add-modules javafx.controls -cp $outDir com.cards.DeckApp $args
exit $LASTEXITCODE
