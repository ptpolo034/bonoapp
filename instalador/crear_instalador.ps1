<#
  Crea el instalador de BonoApp para Windows 10 y 11 (64 bits) con jpackage.

  Uso (desde la carpeta del proyecto, en PowerShell):
      .\instalador\crear_instalador.ps1                      # instalador .exe (el que se entrega)
      .\instalador\crear_instalador.ps1 -Tipo app-image      # carpeta portable, para probar sin instalar
      .\instalador\crear_instalador.ps1 -Tipo msi            # paquete .msi (permite instalar en silencio)
      .\instalador\crear_instalador.ps1 -VersionInstalador 1.0.2   # al sacar una version nueva

  Antes hay que compilar el proyecto (target\bonoapp-1.0.jar). El resultado queda en instalador\salida.

  - Lleva incluido su propio Java reducido (jlink): en el equipo de destino no hay que instalar nada.
  - Se instala solo para el usuario de Windows que lo ejecuta (no pide permisos de administrador) en
    %LOCALAPPDATA%\BonoApp, con acceso directo en el escritorio y en el menu Inicio.
  - Los datos del negocio NO viven en la carpeta del programa sino en %USERPROFILE%\.bonoapp, asi que instalar
    una version nueva encima (o desinstalar) no borra los datos.
  - Instalar una version nueva encima de la anterior la actualiza: el identificador de actualizacion
    (UUID) debe ser SIEMPRE el mismo. No cambiarlo.
  - Requiere WiX Toolset 3.x (para el .exe y el .msi). El instalador no esta firmado digitalmente: Windows mostrara
    "Windows protegio su PC" y hay que pulsar "Mas informacion" y "Ejecutar de todas formas".
#>
param(
    [ValidateSet("exe", "msi", "app-image")][string]$Tipo = "exe",
    # Version numerica del instalador (Windows exige formato 1.0.1). La version "de texto" que ve el usuario
    # (por ejemplo 1.0-prueba1) esta en src\main\java\com\bonoapp\util\Version.java.
    [string]$VersionInstalador = "1.0.1"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$jar = Join-Path $raiz "target\bonoapp-1.0.jar"
$salida = Join-Path $raiz "instalador\salida"
$entrada = Join-Path $salida "entrada"
$icono = Join-Path $raiz "instalador\bonoapp.ico"
$uuidActualizacion = "7d9c3b52-4a1e-4f6b-9c2d-8e1a5b6c0f31"   # NO CAMBIAR: identifica a BonoApp para poder actualizar

if (-not (Test-Path $jar)) { throw "No existe $jar. Compila primero el proyecto (mvn -q package)." }

$jdk = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName "bin\jpackage.exe") } |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $jdk) { throw "No se encontro un JDK con jpackage en C:\Program Files\Java." }
$jpackage = Join-Path $jdk.FullName "bin\jpackage.exe"

$wix = Get-ChildItem "C:\Program Files (x86)", "C:\Program Files" -Directory -Filter "WiX Toolset v3*" -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($wix) { $env:PATH = (Join-Path $wix.FullName "bin") + ";" + $env:PATH }
elseif ($Tipo -ne "app-image") { throw "Falta WiX Toolset 3.x para crear el $Tipo." }

# Carpeta de entrada limpia con solo el jar
if (Test-Path $entrada) { Remove-Item -LiteralPath $entrada -Recurse -Force }
New-Item -ItemType Directory -Force -Path $entrada | Out-Null
Copy-Item $jar $entrada
# Se borra el resultado anterior del mismo tipo para no mezclar versiones
if ($Tipo -eq "app-image") {
    $anterior = Join-Path $salida "BonoApp"
    if (Test-Path $anterior) { Remove-Item -LiteralPath $anterior -Recurse -Force }
} else {
    Get-ChildItem $salida -Filter "BonoApp-*.$Tipo" -ErrorAction SilentlyContinue | Remove-Item -Force
}

$argumentos = @(
    "--type", $Tipo,
    "--input", $entrada,
    "--main-jar", "bonoapp-1.0.jar",
    "--main-class", "com.bonoapp.Main",
    "--name", "BonoApp",
    "--app-version", $VersionInstalador,
    "--vendor", "BonoApp",
    "--copyright", "BonoApp",
    "--description", "Control de boletas, liquidaciones, ahorros y prestamos",
    "--icon", $icono,
    "--dest", $salida,
    # Java reducido: solo los modulos que la app usa (mantiene el instalador pequeno y la app liviana)
    "--add-modules", "java.base,java.desktop,java.prefs,java.sql,java.logging,java.naming,java.xml,jdk.unsupported",
    # Memoria pensada para equipos de 4 GB: la app usa ~100-130 MB; se deja margen hasta 512 MB
    "--java-options", "-Xms32m",
    "--java-options", "-Xmx512m",
    "--java-options", "-XX:+UseSerialGC",
    "--java-options", "-XX:TieredStopAtLevel=1",
    "--java-options", "-Dfile.encoding=UTF-8"
)
if ($Tipo -ne "app-image") {
    $argumentos += @(
        "--win-per-user-install",
        "--win-shortcut",
        "--win-menu",
        "--win-menu-group", "BonoApp",
        "--win-upgrade-uuid", $uuidActualizacion
    )
}

Write-Host "Creando $Tipo con $($jdk.Name)..."
& $jpackage @argumentos
if ($LASTEXITCODE -ne 0) { throw "jpackage fallo (codigo $LASTEXITCODE)." }
Write-Host "Listo. Resultado en: $salida"
Get-ChildItem $salida | Where-Object { $_.Name -ne "entrada" } | ForEach-Object { "  {0}  ({1:N1} MB)" -f $_.Name, (($_ | Get-ChildItem -Recurse -File -ErrorAction SilentlyContinue | Measure-Object Length -Sum).Sum / 1MB + $(if ($_.PSIsContainer) {0} else {$_.Length/1MB})) }
