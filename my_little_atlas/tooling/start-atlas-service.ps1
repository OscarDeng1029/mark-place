param([string]$NodeExecutable='node', [string]$AdbExecutable='adb')
$ErrorActionPreference='Stop'
$atlasProject=Split-Path $PSScriptRoot -Parent
$serviceDirectory=Join-Path $atlasProject 'atlas-service'
if(-not (Test-Path -LiteralPath (Join-Path $serviceDirectory '.env'))) {
    throw 'Configure atlas-service/.env using .env.example first.'
}
$env:PORT='3212'
$env:HOST='127.0.0.1'
$existing=Get-NetTCPConnection -State Listen -LocalPort 3212 -ErrorAction SilentlyContinue
if(-not $existing) {
    Start-Process -FilePath $NodeExecutable -ArgumentList 'server.mjs' -WorkingDirectory $serviceDirectory -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $PSScriptRoot 'atlas-service.stdout.log') `
        -RedirectStandardError (Join-Path $PSScriptRoot 'atlas-service.stderr.log')
}
& $AdbExecutable -s emulator-5554 reverse tcp:3212 tcp:3212
Write-Output 'Atlas API: http://127.0.0.1:3212 (Android emulator USB reverse configured)'
