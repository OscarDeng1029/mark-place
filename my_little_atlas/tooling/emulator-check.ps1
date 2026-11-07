param([ValidateSet('install','share','dump','capture','tap','input','swipe','restart','foreground','diagnostics')][string]$Action = 'dump', [string]$Value = '', [string]$SecondValue = '')
$ErrorActionPreference='Stop'
$atlasProject=Split-Path $PSScriptRoot -Parent
$properties=Get-Content -LiteralPath (Join-Path $atlasProject 'local.properties')
$sdkPath=($properties | Where-Object { $_ -match '^sdk.dir=' }) -replace '^sdk.dir=',''
$adbExecutable=Join-Path $sdkPath 'platform-tools\adb.exe'
$env:ANDROID_USER_HOME=Join-Path $PSScriptRoot 'android-home'
$env:ANDROID_SDK_HOME=Join-Path $PSScriptRoot 'android-sdk-home'
switch($Action) {
    'diagnostics' {
        & $adbExecutable -s emulator-5554 logcat -d -t 250 -s AndroidRuntime ActivityTaskManager
        & $adbExecutable -s emulator-5554 shell dumpsys activity activities | Select-String -Pattern 'mResumedActivity|topResumedActivity|com.mylittleatlas|Intent.*SEND' | Select-Object -Last 15
    }
    'install' {
        & $adbExecutable -s emulator-5554 shell am force-stop com.mylittleatlas.app
        & $adbExecutable -s emulator-5554 install -r (Join-Path $atlasProject 'composeApp\build\outputs\apk\debug\composeApp-debug.apk')
        & $adbExecutable -s emulator-5554 reverse tcp:3212 tcp:3212
    }
    'share' { & $adbExecutable -s emulator-5554 shell am start -n com.mylittleatlas.app/.MainActivity -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT $Value }
    'tap' { & $adbExecutable -s emulator-5554 shell input tap $Value $SecondValue }
    'input' { & $adbExecutable -s emulator-5554 shell input text $Value }
    'swipe' { $coords=$Value.Split(','); & $adbExecutable -s emulator-5554 shell input swipe $coords[0] $coords[1] $coords[2] $coords[3] 450 }
    'restart' { & $adbExecutable -s emulator-5554 shell am force-stop com.mylittleatlas.app; & $adbExecutable -s emulator-5554 shell am start -n com.mylittleatlas.app/.MainActivity }
    'capture' {
        & $adbExecutable -s emulator-5554 shell screencap -p /sdcard/atlas-capture.png
        & $adbExecutable -s emulator-5554 pull /sdcard/atlas-capture.png (Join-Path $atlasProject ('dist\'+$Value+'.png'))
    }
    'dump' {
        & $adbExecutable -s emulator-5554 shell uiautomator dump /sdcard/atlas-ui.xml | Out-Null
        $xmlPath=Join-Path $PSScriptRoot 'tmp\atlas-ui.xml'
        & $adbExecutable -s emulator-5554 pull /sdcard/atlas-ui.xml $xmlPath | Out-Null
        [xml]$atlasUi=Get-Content -Encoding UTF8 -LiteralPath $xmlPath
        $atlasUi.SelectNodes('//node') | Where-Object { $_.text -or $_.'content-desc' } | ForEach-Object {
            [pscustomobject]@{text=$_.text;desc=$_.'content-desc';bounds=$_.bounds}
        } | Format-Table -AutoSize
    }
    'foreground' {
        Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public static class AtlasWindow { [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hwnd); [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hwnd,int command); }'
        Get-Process | Where-Object { $_.MainWindowTitle -like 'Android Emulator - MyLittleAtlas_API_36:*' } | ForEach-Object {
            [AtlasWindow]::ShowWindow($_.MainWindowHandle,9) | Out-Null
            [AtlasWindow]::SetForegroundWindow($_.MainWindowHandle) | Out-Null
            $_ | Select-Object Id,MainWindowTitle
        }
    }
}
