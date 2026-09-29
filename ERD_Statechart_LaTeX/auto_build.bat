@echo off
rem ============================================================
rem  Auto-build: theo doi main.tex, bien dich lai khi co thay doi
rem  (gia lap cong cu build tu dong cho LaTeX tren Windows)
rem ============================================================
setlocal

set XELATEX=C:\Program Files\MiKTeX\miktex\bin\x64\xelatex.exe
set SRC=main.tex

if not exist "%XELATEX%" (
    echo [LOI] Khong tim thay xelatex.exe tai "%XELATEX%"
    echo       Sua bien XELATEX trong file nay cho dung duong dan MiKTeX.
    pause
    exit /b 1
)

echo [auto-build] Dang theo doi %SRC% ... (Ctrl+C de thoat)
set LAST=0

:loop
for %%F in (%SRC%) do set CUR=%%~tF
if not "%CUR%"=="%LAST%" (
    if not "%LAST%"=="0" (
        echo.
        echo [auto-build] Phat hien thay doi %CUR% -- bien dich lai...
        "%XELATEX%" -interaction=nonstopmode -synctex=1 %SRC% >nul 2>&1
        "%XELATEX%" -interaction=nonstopmode -synctex=1 %SRC% >nul 2>&1
        echo [auto-build] Xong. main.pdf da cap nhat %time:~0,8%.
    )
    set LAST=%CUR%
)
timeout /t 2 /nobreak >nul
goto loop
