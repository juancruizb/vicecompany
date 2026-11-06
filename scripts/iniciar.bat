@echo off
REM Inicia ViceCompany con un JDK 21 portable guardado junto a este archivo.
REM No modifica el Java 8 del servidor ni el PATH.
cd /d "%~dp0"
set "JAVA="
call :buscar_java

if not defined JAVA (
  if not exist "jdk21.zip" (
    echo Falta jdk21.zip. Copiarlo a esta carpeta.
    pause
    exit /b 1
  )
  echo Primera vez: descomprimiendo el JDK 21 portable, puede tardar un minuto...
  powershell -NoProfile -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory('jdk21.zip', '.')"
  call :buscar_java
)

if not defined JAVA (
  echo No se encontro el JDK. Descomprimir jdk21.zip a mano en esta carpeta.
  pause
  exit /b 1
)

"%JAVA%" -version
echo.
echo Abrir http://localhost:8090/login cuando diga "Started ViceCompanyApplication".
echo Para detener: Ctrl+C o cerrar esta ventana.
echo.
REM -Xmx512m: limita la memoria a 512 MB para no competir con Tomcat y SQL Server.
"%JAVA%" -Xmx512m -jar vicecompany.jar
pause
exit /b

REM Busca java.exe en jdk\ o en la carpeta tal como sale del zip (jdk-21...), sin renombrar.
:buscar_java
if exist "jdk\bin\java.exe" set "JAVA=%~dp0jdk\bin\java.exe"
if defined JAVA exit /b
for /d %%D in (jdk-21*) do if exist "%%D\bin\java.exe" set "JAVA=%~dp0%%D\bin\java.exe"
exit /b
