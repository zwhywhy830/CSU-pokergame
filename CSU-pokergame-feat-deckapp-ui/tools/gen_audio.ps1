# 阶段 21：合成棋牌室音效 / 背景音乐（纯 WAV，无外部依赖）
# 用法：Write-Wav <输出路径> <扁平音符表 f,d,v,f,d,v,...>
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\src\main\resources\audio'
$effDir = Join-Path $root 'effect'
$bgmDir = Join-Path $root 'bgm'
New-Item -ItemType Directory -Force -Path $effDir, $bgmDir | Out-Null

$SR = 22050

function Write-Wav([string]$path, [double[]]$notes) {
    $buf = New-Object System.Collections.Generic.List[double]
    for ($k = 0; $k + 2 -lt $notes.Length; $k += 3) {
        $f = [double]$notes[$k]
        $d = [double]$notes[$k + 1]
        $v = [double]$notes[$k + 2]
        $cnt = [int]($SR * $d)
        for ($i = 0; $i -lt $cnt; $i++) {
            $t = $i / $SR
            $env = 1.0
            if ($t -lt 0.004) { $env = $t / 0.004 }
            $rel = ($d - $t) / 0.012
            if ($rel -lt 1) { $env = [Math]::Min($env, [Math]::Max(0.0, $rel)) }
            $s = [Math]::Sin(2 * [Math]::PI * $f * $t)
            $s += 0.25 * [Math]::Sin(4 * [Math]::PI * $f * $t)
            $buf.Add($s * $v * $env)
        }
    }
    $n = $buf.Count
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    $bw.Write([char[]]'RIFF'); $bw.Write([int](36 + $n * 2)); $bw.Write([char[]]'WAVE')
    $bw.Write([char[]]'fmt '); $bw.Write([int]16); $bw.Write([int16]1); $bw.Write([int16]1)
    $bw.Write([int]$SR); $bw.Write([int]($SR * 2)); $bw.Write([int16]2); $bw.Write([int16]16)
    $bw.Write([char[]]'data'); $bw.Write([int]($n * 2))
    foreach ($x in $buf) {
        $bw.Write([int16]([Math]::Max(-32000, [Math]::Min(32000, $x * 24000))))
    }
    $bw.Flush()
    [System.IO.File]::WriteAllBytes($path, $ms.ToArray())
    $bw.Dispose(); $ms.Dispose()
    Write-Host ("{0,-20} {1,8} bytes" -f (Split-Path $path -Leaf), (Get-Item $path).Length)
}

Write-Wav (Join-Path $effDir 'button_click.wav')  @(1250, 0.045, 0.55)
Write-Wav (Join-Path $effDir 'card_pick.wav')     @(720, 0.060, 0.50)
Write-Wav (Join-Path $effDir 'card_play.wav')     @(520, 0.06, 0.50, 860, 0.09, 0.45)
Write-Wav (Join-Path $effDir 'win.wav')           @(523, 0.12, 0.40, 659, 0.12, 0.40, 784, 0.12, 0.40, 1046, 0.26, 0.45)
Write-Wav (Join-Path $effDir 'lose.wav')          @(494, 0.14, 0.40, 392, 0.14, 0.40, 294, 0.30, 0.40)
Write-Wav (Join-Path $effDir 'coin.wav')          @(1400, 0.05, 0.45, 1900, 0.12, 0.40)
Write-Wav (Join-Path $effDir 'level_up.wav')      @(523, 0.10, 0.40, 659, 0.10, 0.40, 784, 0.10, 0.40, 988, 0.10, 0.40, 1318, 0.32, 0.45)
Write-Wav (Join-Path $effDir 'achievement.wav')   @(880, 0.09, 0.40, 1174, 0.09, 0.40, 1568, 0.28, 0.42)

# 背景音乐：16 拍轻快循环，主旋律 + 低八度伴奏
$melody = @(523, 587, 659, 587, 523, 659, 784, 659, 587, 523, 587, 659, 523, 494, 523, 587)
$bgm = New-Object System.Collections.Generic.List[double]
foreach ($m in $melody) {
    $bgm.Add([double]$m)
    $bgm.Add(0.30)
    $bgm.Add(0.14)
    $bgm.Add([double]$m / 2.0)
    $bgm.Add(0.30)
    $bgm.Add(0.08)
}
Write-Wav (Join-Path $bgmDir 'lobby.wav') $bgm.ToArray()

Write-Host 'AUDIO_ASSETS_OK'
