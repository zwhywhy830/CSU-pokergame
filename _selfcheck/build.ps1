$p = 'd:\AAAAprogram\game\CSU-pokergame-feat-deckapp-ui'
$out = Join-Path $p 'out'
$lib = Join-Path $p 'lib'
$jar = (Get-ChildItem (Join-Path $lib 'jackson') -Filter *.jar | ForEach-Object FullName) -join ';'
Copy-Item (Join-Path $p 'src\main\resources\app.css') $out -Force
Copy-Item (Join-Path $p 'src\main\resources\audio') (Join-Path $out 'audio') -Recurse -Force
$srcs = @(Get-ChildItem (Join-Path $p 'src\main\java') -Recurse -Filter *.java | ForEach-Object FullName)
& javac --module-path $lib --add-modules javafx.controls,javafx.media -cp $jar -encoding UTF-8 -parameters -d $out $srcs 2>&1 | Select-Object -First 20
Write-Output ("javac exit=" + $LASTEXITCODE)
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Output "COMPILE_OK"
$s = 'd:\AAAAprogram\game\_selfcheck'
$cp = ($s, $out, $jar) -join ';'
& javac --module-path $lib --add-modules javafx.controls,javafx.media -cp ($out + ';' + $jar) -encoding UTF-8 -d $s (Join-Path $s 'ProfileShot.java') (Join-Path $s 'StatsSelfCheck.java') (Join-Path $s 'GameRecordSelfCheck.java') (Join-Path $s 'LeaderboardSelfCheck.java') (Join-Path $s 'StatisticsSelfCheck.java') (Join-Path $s 'SettingsSelfCheck.java') (Join-Path $s 'AudioSelfCheck.java') (Join-Path $s 'AudioShot.java') (Join-Path $s 'AnimationSelfCheck.java') (Join-Path $s 'AnimationShot.java') (Join-Path $s 'PdkUISelfCheck.java') (Join-Path $s 'PdkUIShot.java') (Join-Path $s 'LiarUISelfCheck.java') (Join-Path $s 'LiarUIShot.java') 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp GameRecordSelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp LeaderboardSelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp StatisticsSelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp SettingsSelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp AudioSelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp AnimationSelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp AnimationShot 'd:\AAAAprogram\game\stage22_animation.png' 2>&1 | Select-String 'AnimationShot'
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp PdkUISelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp PdkUIShot 'd:\AAAAprogram\game\stage23_pdk_table.png' 2>&1 | Select-String 'PdkUIShot'
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp PdkUIShot 'd:\AAAAprogram\game\stage23_pdk_toast.png' toast 2>&1 | Select-String 'PdkUIShot'
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp LiarUISelfCheck 2>&1
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp LiarUIShot 'd:\AAAAprogram\game\stage24_liar_table.png' 2>&1 | Select-String 'LiarUIShot'
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp LiarUIShot 'd:\AAAAprogram\game\stage24_liar_fx.png' fx 2>&1 | Select-String 'LiarUIShot'
& java --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp ProfileShot 'd:\AAAAprogram\game\stage12_profile.png' 2>&1 | Select-String 'ProfileShot'
$tmp = Join-Path $s 'tmp'
$args2 = @("-Dplayer.data.file=$tmp\player.json", "-Dcoin.log.file=$tmp\coin_log.json")
& java $args2 --enable-native-access=javafx.graphics --module-path $lib --add-modules javafx.controls,javafx.media -cp $cp ProfileShot 'd:\AAAAprogram\game\stage12_profile_unlocked.png' 2>&1