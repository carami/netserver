# PowerShell 스크립트 - UTF-8 설정
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "콘솔을 UTF-8 모드로 설정했습니다." -ForegroundColor Green
Write-Host ""

# Java 실행 시 UTF-8 인코딩 옵션 추가
java -Dfile.encoding=UTF-8 `
     -Dconsole.encoding=UTF-8 `
     -Dsun.stdout.encoding=UTF-8 `
     -Dsun.stderr.encoding=UTF-8 `
     com.example.net.SimpleEchoClient $args

Read-Host "Press Enter to continue..."