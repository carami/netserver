@echo off
chcp 65001 > nul
echo 콘솔을 UTF-8 모드로 설정했습니다.
echo.

REM Java 실행 시 UTF-8 인코딩 옵션 추가
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 com.example.net.ChatServer %*

pause