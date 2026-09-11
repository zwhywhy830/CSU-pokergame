$p = 'd:\AAAAprogram\game\CSU-pokergame-feat-deckapp-ui'
$lib = Join-Path $p 'lib'
$out = Join-Path $p 'out'
$jar = (Get-ChildItem (Join-Path $lib 'jackson') -Filter *.jar | ForEach-Object FullName) -join ';'
Copy-Item (Join-Path $p 'src\main\resources\app.css') $out -Force
$jargs = @('--enable-native-access=javafx.graphics', '--module-path', $lib, '--add-modules',
    'javafx.controls,javafx.media', '-cp', ($out + ';' + $jar), 'com.cards.DeckApp')
Start-Process -FilePath 'java' -ArgumentList $jargs -WorkingDirectory $out `
    -RedirectStandardError (Join-Path $out 'app_err.log') `
    -RedirectStandardOutput (Join-Path $out 'app_out.log')
Write-Output 'LAUNCHED'
