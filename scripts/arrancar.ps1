<#
    scripts/arrancar.ps1
    --------------------
    Pone el servicio en marcha por cualquiera de los tres caminos, con una sola
    orden y sin pasos manuales previos.

        .\scripts\arrancar.ps1                  sin Docker  (H2 en archivo)
        .\scripts\arrancar.ps1 -Modo docker     con Docker  (PostgreSQL 16 real)
        .\scripts\arrancar.ps1 -Modo nube       comprueba el despliegue en Render

    Tambien sirve para partir de cero en una maquina donde no hay nada: si se
    ejecuta fuera del repositorio, lo clona antes.

        powershell -ExecutionPolicy Bypass -File .\arrancar.ps1

    OPCIONES

        -Modo <sin-docker|docker|nube>  camino de ejecucion (por defecto sin-docker)
        -Raiz <carpeta>                 donde clonar, si hay que clonar
        -Puerto <n>                     puerto de escucha (por defecto 8080)
        -SoloPreparar                   deja todo listo sin arrancar
        -Limpiar                        borra la base local y empieza de cero
        -Pruebas                        ejecuta la suite completa y termina
        -SinNavegador                   no abre el navegador

    QUE HACE FALTA EN LA MAQUINA

        sin-docker   Git y un JDK 21 o posterior. Nada mas: Maven viene con el
                     proyecto y la base es un archivo.
        docker       Git y Docker Desktop. El `.env` lo genera este script.
        nube         nada; solo consulta el servicio publicado.

    Es idempotente. Si el repositorio ya esta clonado lo actualiza, y si hay
    cambios locales sin confirmar no los toca.
#>

[CmdletBinding()]
param(
    [ValidateSet("sin-docker", "docker", "nube")]
    [string] $Modo = "sin-docker",
    [string] $Raiz,
    [string] $Rama = "main",
    [int]    $Puerto = 8080,
    [switch] $SoloPreparar,
    [switch] $Limpiar,
    [switch] $Pruebas,
    [switch] $SinNavegador
)

$ErrorActionPreference = "Stop"

$REPO_URL = "https://github.com/rcarmonan-commits/tcc-transcaribe-pqrs.git"
$CARPETA  = "tcc-transcaribe-pqrs"
$URL_NUBE = "https://transcaribe-pqrs-api.onrender.com"

# PowerShell 5.1 no define $IsWindows; si no existe, es Windows.
$esWindows = if (Test-Path variable:IsWindows) { $IsWindows } else { $true }

# ---------------------------------------------------------------- utilidades

function Paso    ($t) { Write-Host ""; Write-Host "==> $t" -ForegroundColor Cyan }
function Bien    ($t) { Write-Host "    $t" -ForegroundColor Green }
function Aviso   ($t) { Write-Host "    $t" -ForegroundColor Yellow }
function Detalle ($t) { Write-Host "    $t" -ForegroundColor DarkGray }

function Parar ($mensaje, $remedio) {
    Write-Host ""
    Write-Host "  $mensaje" -ForegroundColor Red
    if ($remedio) {
        Write-Host ""
        Write-Host "  Como resolverlo:" -ForegroundColor Yellow
        $remedio -split "`n" | ForEach-Object { Write-Host "    $_" }
    }
    Write-Host ""
    exit 1
}

function Existe ($comando) {
    $null -ne (Get-Command $comando -ErrorAction SilentlyContinue)
}

# Read-Host revienta cuando la consola no es interactiva (tareas programadas,
# tuberias, integracion continua). Ahi no se pregunta: se responde que no.
function Preguntar ($texto) {
    try   { return (Read-Host $texto) }
    catch { return "" }
}

function Aleatorio ($bytes) {
    $b = New-Object byte[] $bytes
    $g = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $g.GetBytes($b)
    return (($b | ForEach-Object { $_.ToString("x2") }) -join "")
}

function PuertoOcupado ($n) {
    if (Existe "Get-NetTCPConnection") {
        try {
            return ($null -ne (Get-NetTCPConnection -LocalPort $n -State Listen -ErrorAction SilentlyContinue))
        } catch { }
    }
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $r = $c.BeginConnect("127.0.0.1", $n, $null, $null)
        if ($r.AsyncWaitHandle.WaitOne(400)) { $c.EndConnect($r); $c.Close(); return $true }
        $c.Close()
    } catch { }
    return $false
}

function AbrirNavegador ($url) {
    try {
        if     ($esWindows)        { Start-Process $url }
        elseif (Existe "open")     { & open $url }
        elseif (Existe "xdg-open") { & xdg-open $url }
    } catch { }
}

function AbrirCuandoResponda ($url, $salud) {
    if ($SinNavegador) { return }
    Start-Job -ScriptBlock {
        param($url, $salud, $windows)
        for ($i = 0; $i -lt 120; $i++) {
            Start-Sleep -Seconds 2
            try {
                $r = Invoke-WebRequest -Uri $salud -UseBasicParsing -TimeoutSec 3
                if ($r.StatusCode -eq 200) {
                    if ($windows)                                              { Start-Process $url }
                    elseif (Get-Command open -ErrorAction SilentlyContinue)     { & open $url }
                    elseif (Get-Command xdg-open -ErrorAction SilentlyContinue) { & xdg-open $url }
                    break
                }
            } catch { }
        }
    } -ArgumentList $url, $salud, $esWindows | Out-Null
}

# Version mayor del JDK de una ruta concreta, o 0 si no sirve.
function VersionDeJava ($ejecutable) {
    if (-not $ejecutable -or -not (Test-Path $ejecutable)) { return 0 }
    try   { $salida = & $ejecutable -version 2>&1 | Out-String }
    catch { return 0 }
    # "21.0.5" en los modernos, "1.8.0_392" en los antiguos
    if ($salida -match 'version "(\d+)\.(\d+)') {
        $mayor = [int]$Matches[1]
        if ($mayor -eq 1) { return [int]$Matches[2] }
        return $mayor
    }
    return 0
}

# Busca un JDK 21 o posterior sin dar por supuesta la instalacion del equipo.
#
# El orden importa. Primero JAVA_HOME, porque es lo que Maven consulta; luego
# el `java` del PATH; y solo despues las carpetas de los instaladores. Hay
# equipos donde el `java` del PATH es un Java 8 viejo de Oracle mientras el JDK
# bueno vive en JAVA_HOME sin estar en el PATH: mirar solo el PATH daria un
# diagnostico equivocado y mandaria a instalar algo que ya esta.
function BuscarJdk {
    $candidatos = @()

    if ($env:JAVA_HOME) { $candidatos += $env:JAVA_HOME }

    $enPath = Get-Command java -ErrorAction SilentlyContinue
    if ($enPath) { $candidatos += (Split-Path (Split-Path $enPath.Source -Parent) -Parent) }

    if ($esWindows) {
        $patrones = @("$env:ProgramFiles\OpenJDK\jdk-*",
                      "$env:ProgramFiles\Microsoft\jdk-*",
                      "$env:ProgramFiles\Eclipse Adoptium\jdk-*",
                      "$env:ProgramFiles\Java\jdk-*",
                      "$env:ProgramFiles\Amazon Corretto\jdk*",
                      "$env:ProgramFiles\BellSoft\LibericaJDK-*",
                      "$env:ProgramFiles\Zulu\zulu-*",
                      "$env:ProgramFiles\RedHat\java-*",
                      "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-*")
    } else {
        $patrones = @("/usr/lib/jvm/*",
                      "/Library/Java/JavaVirtualMachines/*/Contents/Home",
                      "$HOME/.sdkman/candidates/java/*",
                      "/opt/homebrew/opt/openjdk*",
                      "/usr/local/opt/openjdk*")
    }
    foreach ($patron in $patrones) {
        $candidatos += (Get-ChildItem -Path $patron -Directory -ErrorAction SilentlyContinue |
                        Sort-Object Name -Descending | ForEach-Object { $_.FullName })
    }

    $binario = if ($esWindows) { "bin\java.exe" } else { "bin/java" }
    $mejorRuta = $null; $mejorVersion = 0
    foreach ($raiz in $candidatos) {
        if (-not $raiz) { continue }
        $v = VersionDeJava (Join-Path $raiz $binario)
        if ($v -ge 21) { return [pscustomobject]@{ Ruta = $raiz; Version = $v } }
        if ($v -gt $mejorVersion) { $mejorVersion = $v; $mejorRuta = $raiz }
    }
    return [pscustomobject]@{ Ruta = $mejorRuta; Version = $mejorVersion }
}

function ExigirGit {
    if (Existe "git") { return }
    $comoInstalar = if ($esWindows) { "winget install --id Git.Git -e" }
                    elseif (Existe "brew") { "brew install git" }
                    else { "sudo apt install git     (o el gestor de su distribucion)" }
    Parar "No se encontro Git." @"
$comoInstalar
Cierre y vuelva a abrir la terminal, y lance el script otra vez.
"@
}

function ExigirJdk {
    $jdk = BuscarJdk

    if ($jdk.Version -lt 21 -and $esWindows -and (Existe "winget")) {
        Write-Host ""
        if ($jdk.Version -eq 0) {
            Aviso "No se encontro ningun Java. El proyecto compila con la version 21."
        } else {
            Aviso ("El Java de este equipo es la version " + $jdk.Version + ". El proyecto compila con la 21.")
        }
        if ((Preguntar "    Instalar Microsoft OpenJDK 21 ahora con winget? [s/N]") -match '^[sSyY]') {
            Paso "Instalando el JDK 21 (tarda unos minutos)"
            & winget install --id Microsoft.OpenJDK.21 -e --accept-source-agreements --accept-package-agreements
            $jdk = BuscarJdk
        }
    }

    if ($jdk.Version -lt 21) {
        $comoInstalar = if ($esWindows) {
            "winget install --id Microsoft.OpenJDK.21 -e`nSin winget: https://adoptium.net/es/temurin/releases/?version=21"
        } elseif (Existe "brew") { "brew install openjdk@21" }
        else { "sudo apt install openjdk-21-jdk     (o: sdk install java 21-tem)" }
        Parar "Se necesita un JDK 21 o posterior y no se encontro ninguno." @"
$comoInstalar

Cierre y vuelva a abrir la terminal, y lance el script otra vez.
Si ya lo tiene instalado, basta con apuntar JAVA_HOME a su carpeta.
"@
    }

    # Se fija para ESTA sesion y nada mas. Un script que levanta una
    # demostracion no tiene por que cambiarle la configuracion a nadie.
    $env:JAVA_HOME = $jdk.Ruta
    $env:PATH = (Join-Path $jdk.Ruta "bin") + [IO.Path]::PathSeparator + $env:PATH
    Detalle ("java  " + $jdk.Version + "   " + $jdk.Ruta)
    if ($jdk.Version -gt 21) { Detalle "Posterior a la 21; el proyecto se compila igualmente para la 21." }
}

# ------------------------------------------------------------------ portada

Write-Host ""
Write-Host "  API de PQRS - Transcaribe S.A." -ForegroundColor White
Write-Host ("  Modo: " + $Modo) -ForegroundColor DarkGray
Write-Host "  ------------------------------------------------------------"

# --------------------------------------------- donde esta el codigo fuente

# Si el script vive dentro del repositorio, no hay nada que clonar.
$aqui = Split-Path -Parent $MyInvocation.MyCommand.Path
$posible = Split-Path -Parent $aqui
$dentroDelRepo = (Test-Path (Join-Path $posible "pom.xml")) -and (Test-Path (Join-Path $posible "mvnw"))

if ($dentroDelRepo) {
    $proyecto = $posible
} else {
    if (-not $Raiz) { $Raiz = Join-Path $HOME "proyectos" }
    $proyecto = Join-Path $Raiz $CARPETA
}

if ($Modo -ne "nube") {
    Paso "Codigo fuente"
    ExigirGit

    if ($dentroDelRepo) {
        Detalle "Ejecutandose dentro del repositorio: no hay que clonar."
        Detalle $proyecto
    } else {
        if (-not (Test-Path $Raiz)) {
            New-Item -ItemType Directory -Path $Raiz -Force | Out-Null
            Bien "Creada  $Raiz"
        }

        # Windows corta las rutas en 260 caracteres salvo que se le diga lo
        # contrario, y la ruta de paquetes de este proyecto es profunda: solo
        #   src/main/java/co/edu/unicartagena/transcaribe/pqrs/infrastructure/
        #   persistence/jpa/ConsecutivoRadicadoJpaRepository.java
        # ya son 155 caracteres. Clonar en una carpeta de nombre largo hace que
        # el checkout falle a medias y deje el repositorio incompleto, con un
        # mensaje que no menciona la longitud.
        $opcionesGit = @()
        if ($esWindows) {
            $opcionesGit = @("-c", "core.longpaths=true")
            $margen = 260 - ($proyecto.Length + 1 + 155)
            if ($margen -lt 0) {
                Aviso ("Ruta de destino larga: supera en " + [Math]::Abs($margen) + " caracteres el limite de Windows.")
                Detalle "Se activan las rutas largas de Git. Si aun asi falla, use -Raiz C:\cea"
            }
        }

        if (Test-Path (Join-Path $proyecto ".git")) {
            Push-Location $proyecto
            try {
                if (& git status --porcelain) {
                    Aviso "Hay cambios locales sin confirmar: se dejan como estan."
                } else {
                    & git fetch origin --quiet
                    & git checkout $Rama --quiet
                    & git pull --ff-only origin $Rama --quiet
                    Bien ("Actualizado a " + (& git rev-parse --short HEAD) + " en " + $Rama)
                }
            } finally { Pop-Location }
        } else {
            if (Test-Path $proyecto) {
                Parar "La carpeta existe pero no es un repositorio de Git." "$proyecto`nBorrela o renombrela, y vuelva a lanzar el script."
            }
            Detalle "Clonando desde $REPO_URL"
            & git @opcionesGit clone --branch $Rama $REPO_URL $proyecto
            if ($LASTEXITCODE -ne 0) {
                if (Test-Path $proyecto) { Remove-Item $proyecto -Recurse -Force -ErrorAction SilentlyContinue }
                Parar "No se pudo clonar el repositorio." @"
Compruebe la conexion a internet.
Si el mensaje dice 'Filename too long', es el limite de rutas de Windows:
clone en una carpeta mas corta con  -Raiz C:\cea
"@
            }
            if ($esWindows) {
                Push-Location $proyecto
                try { & git config core.longpaths true } finally { Pop-Location }
            }
            Bien "Clonado en $proyecto"
        }
    }
    Set-Location $proyecto
}

# ------------------------------- configuraciones de IntelliJ y VS Code

function PrepararEntornoDeDesarrollo {
    # .idea/ y .vscode/ estan en .gitignore: esto no ensucia el repositorio.
    $dirRun = Join-Path $proyecto (Join-Path ".idea" "runConfigurations")
    New-Item -ItemType Directory -Path $dirRun -Force | Out-Null

    $xml = @'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="PQRS local (sin Docker)" type="MavenRunConfiguration" factoryName="Maven">
    <MavenSettings>
      <option name="myGeneralSettings" />
      <option name="myRunnerSettings" />
      <option name="myRunnerParameters">
        <MavenRunnerParameters>
          <option name="cmdOptions" />
          <option name="profiles">
            <set>
              <option value="local" />
            </set>
          </option>
          <option name="goals">
            <list>
              <option value="spring-boot:run" />
            </list>
          </option>
          <option name="profilesMap">
            <map>
              <entry key="local" value="true" />
            </map>
          </option>
          <option name="projectsCmdOptionValues">
            <list />
          </option>
          <option name="resolveToWorkspace" value="false" />
          <option name="workingDirPath" value="$PROJECT_DIR$" />
        </MavenRunnerParameters>
      </option>
    </MavenSettings>
    <method v="2" />
  </configuration>
</component>
'@
    Set-Content -Path (Join-Path $dirRun "PQRS_local.xml") -Value $xml -Encoding UTF8

    $dirCode = Join-Path $proyecto ".vscode"
    New-Item -ItemType Directory -Path $dirCode -Force | Out-Null

    $tareas = @'
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "PQRS local (sin Docker)",
      "type": "shell",
      "command": "./mvnw",
      "args": ["-Plocal", "spring-boot:run"],
      "windows": { "command": ".\\mvnw.cmd" },
      "problemMatcher": [],
      "isBackground": true,
      "group": { "kind": "build", "isDefault": true },
      "presentation": { "reveal": "always", "panel": "dedicated" }
    },
    {
      "label": "PQRS con Docker",
      "type": "shell",
      "command": "docker",
      "args": ["compose", "up", "--build"],
      "problemMatcher": [],
      "isBackground": true
    },
    {
      "label": "PQRS pruebas",
      "type": "shell",
      "command": "./mvnw",
      "args": ["verify"],
      "windows": { "command": ".\\mvnw.cmd" },
      "problemMatcher": [],
      "group": { "kind": "test", "isDefault": true }
    }
  ]
}
'@
    Set-Content -Path (Join-Path $dirCode "tasks.json") -Value $tareas -Encoding UTF8

    $ajustes = @'
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "files.exclude": { "**/target": true, "**/datos": true }
}
'@
    Set-Content -Path (Join-Path $dirCode "settings.json") -Value $ajustes -Encoding UTF8

    Bien "IntelliJ: 'PQRS local (sin Docker)' en el desplegable de ejecucion."
    Bien "VS Code: Terminal > Ejecutar tarea > 'PQRS local (sin Docker)'."
    Detalle ".idea/ y .vscode/ estan en .gitignore: no se versionan."
}

# ------------------------------------------------- resumen de como entrar

function ComoEntrar ($base, $conDatosDemo) {
    Write-Host ""
    Write-Host "  ------------------------------------------------------------"
    Write-Host "  Listo. Asi se entra:" -ForegroundColor White
    Write-Host ""
    Write-Host ("    Swagger      " + $base + "/swagger-ui.html") -ForegroundColor Cyan
    Write-Host ("    API          " + $base + "/api/v1")
    Write-Host ("    Salud        " + $base + "/actuator/health")
    Write-Host ""
    if ($conDatosDemo) {
        Write-Host "    Cuentas de demostracion:"
        Write-Host "      rcarmonan   Transc@ribe2026.Adm    ADMIN + FUNCIONARIO"
        Write-Host "      jarnedoz    Transc@ribe2026.Fun    FUNCIONARIO"
        Write-Host "      mortegas4   Transc@ribe2026.Aud    AUDITOR"
        Write-Host ""
        Write-Host "    Cargados: 7 rutas, 6 vehiculos y los catalogos. Las PQRS no:"
        Write-Host "    se radican desde POST /api/v1/pqrs, que es publico."
    } else {
        Write-Host "    Sin datos de demostracion: este perfil no los carga."
        Write-Host "    La cuenta inicial sale de ADMIN_USUARIO / ADMIN_CONTRASENA."
    }
    Write-Host ""
    Write-Host "    En Swagger: POST /api/v1/autenticacion/iniciar-sesion, copie"
    Write-Host "    el campo 'token', pulse Authorize y peguelo. Sin 'Bearer'."
    Write-Host "  ------------------------------------------------------------"
}

# =====================================================  MODO: NUBE

if ($Modo -eq "nube") {
    Paso "Comprobando el servicio publicado"
    Detalle $URL_NUBE
    Detalle "En el plan gratuito de Render el servicio se duerme: la primera"
    Detalle "peticion puede tardar hasta un minuto en despertarlo."

    $viva = $false
    try {
        $r = Invoke-WebRequest -Uri "$URL_NUBE/actuator/health" -UseBasicParsing -TimeoutSec 120
        if ($r.StatusCode -eq 200) { $viva = $true; Bien ("Responde: " + $r.Content) }
    } catch {
        Aviso "No respondio dentro del plazo."
    }

    if ($viva) {
        ComoEntrar $URL_NUBE $false
        if (-not $SinNavegador) { AbrirNavegador "$URL_NUBE/swagger-ui.html" }
    } else {
        Write-Host ""
        Write-Host "  Para desplegarlo de nuevo, o en otra cuenta:" -ForegroundColor Yellow
        Detalle "1. En Render: New > Blueprint, y apunte al repositorio."
        Detalle "2. Render lee render.yaml y pide las variables marcadas sync: false."
        Detalle "   Son DATABASE_URL (la cadena interna de la base) y las tres"
        Detalle "   ADMIN_*, que crean la primera cuenta."
        Detalle "3. OJO: esas variables solo se piden al CREAR el blueprint. Si ya"
        Detalle "   existe, se editan en Environment y se relanza el despliegue."
        Write-Host ""
    }
    exit 0
}

# =====================================================  MODO: DOCKER

if ($Modo -eq "docker") {
    Paso "Comprobando Docker"
    if (-not (Existe "docker")) {
        Parar "No se encontro Docker." @"
Instalelo desde https://www.docker.com/products/docker-desktop/
o, si prefiere no usar Docker, arranque con:
  .\scripts\arrancar.ps1
"@
    }
    & docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Parar "Docker esta instalado pero el demonio no responde." @"
Abra Docker Desktop y espere a que el icono deje de moverse.
Si no quiere esperar, el modo sin Docker no lo necesita:
  .\scripts\arrancar.ps1
"@
    }
    Bien ("docker  " + (& docker version --format '{{.Server.Version}}'))

    Paso "Archivo de entorno"
    $env_ruta = Join-Path $proyecto ".env"
    if (Test-Path $env_ruta) {
        Detalle ".env ya existe: se respeta tal cual esta."
    } else {
        # Se genera con credenciales nuevas en vez de copiar el ejemplo tal
        # cual. Un .env de ejemplo con contrasena de ejemplo acaba llegando a
        # algun sitio donde no deberia, y ademas obligaria a editarlo a mano
        # antes de poder arrancar, que es justo lo que este script evita.
        $clave = Aleatorio 18
        $firma = Aleatorio 32
        $contenido = @"
# Generado por scripts/arrancar.ps1. Esta en .gitignore: no se versiona.
# Credenciales nuevas para esta maquina; no sirven en ningun otro sitio.

DB_NOMBRE=transcaribe_pqrs
DB_USUARIO=transcaribe
DB_CONTRASENA=$clave

JWT_SECRET=$firma

PUERTO=$Puerto
# El perfil dev carga ademas los datos de demostracion. Para probar el camino
# de produccion, ponga prod y defina ADMIN_USUARIO, ADMIN_CORREO y ADMIN_CONTRASENA.
PERFIL=dev
CORS_ORIGENES=http://localhost:4200
RATE_LIMIT_POR_MINUTO=120
"@
        Set-Content -Path $env_ruta -Value $contenido -Encoding UTF8
        Bien ".env generado con credenciales propias de esta maquina."
        Detalle "Perfil dev: carga el esquema y los datos de demostracion."
    }

    PrepararEntornoDeDesarrollo

    if ($Limpiar) {
        Paso "Borrando los volumenes anteriores"
        & docker compose down -v
        Bien "Base anterior borrada."
    }

    $urlBase = "http://localhost:$Puerto"
    ComoEntrar $urlBase $true

    if ($SoloPreparar) {
        Write-Host ""
        Bien "Preparado sin arrancar. Para levantarlo:"
        Detalle "cd $proyecto"
        Detalle "docker compose up --build"
        Write-Host ""
        exit 0
    }

    Paso "Levantando los contenedores (la primera vez compila la imagen)"
    Detalle "Para detenerlo: Ctrl+C, y despues  docker compose down"
    AbrirCuandoResponda "$urlBase/swagger-ui.html" "$urlBase/actuator/health"
    & docker compose up --build
    Write-Host ""
    Detalle "Contenedores detenidos."
    Get-Job -ErrorAction SilentlyContinue | Remove-Job -Force -ErrorAction SilentlyContinue
    exit 0
}

# =====================================================  MODO: SIN DOCKER

Paso "Comprobando lo que hace falta en el equipo"
Detalle ("git   " + ((& git --version) -replace 'git version ', ''))
ExigirJdk
Bien "Requisitos cubiertos. Maven no hace falta: el proyecto trae su envoltorio."

Paso "Base de datos local"
$carpetaDatos = Join-Path $proyecto "datos"
if ($Limpiar -and (Test-Path $carpetaDatos)) {
    Remove-Item -Path $carpetaDatos -Recurse -Force
    Bien "Base anterior borrada. Se recreara desde las migraciones."
}
if (-not (Test-Path $carpetaDatos)) {
    New-Item -ItemType Directory -Path $carpetaDatos -Force | Out-Null
    Detalle "Creada  datos/   (archivo H2; esta en .gitignore)"
} else {
    Detalle "Ya existe  datos/   (se conserva lo radicado antes)"
}
Detalle "Flyway aplicara db/migration + db/demo al arrancar."

Paso "Configuraciones para el entorno de desarrollo"
PrepararEntornoDeDesarrollo

$mvnw = if ($esWindows) { Join-Path $proyecto "mvnw.cmd" } else { Join-Path $proyecto "mvnw" }
if (-not $esWindows -and (Test-Path $mvnw)) { try { & chmod +x $mvnw } catch { } }

if ($Pruebas) {
    Paso "Ejecutando la suite completa"
    & $mvnw verify
    if ($LASTEXITCODE -ne 0) { Parar "Las pruebas fallaron. Revise la salida de arriba." $null }
    Bien "Suite en verde."
    Write-Host ""
    exit 0
}

Paso "Compilando (la primera vez descarga las dependencias: puede tardar)"
& $mvnw -q -Plocal -DskipTests package
if ($LASTEXITCODE -ne 0) {
    Parar "La compilacion fallo." @"
Si el error menciona 'Could not transfer' o 'status code: 429', es la red o un
limite de descargas de Maven Central: espere un par de minutos y repita.
"@
}
Bien "Compilado."

$urlBase = "http://localhost:$Puerto"
ComoEntrar $urlBase $true

if ($SoloPreparar) {
    Write-Host ""
    Bien "Preparado sin arrancar. Para levantarlo:"
    Detalle "cd $proyecto"
    $orden = if ($esWindows) { ".\mvnw.cmd -Plocal spring-boot:run" } else { "./mvnw -Plocal spring-boot:run" }
    Detalle $orden
    Write-Host ""
    exit 0
}

if (PuertoOcupado $Puerto) {
    Parar "El puerto $Puerto ya esta ocupado." @"
Use otro:   .\scripts\arrancar.ps1 -Puerto 9090
O averigue quien lo tiene:   Get-NetTCPConnection -LocalPort $Puerto -State Listen
"@
}

Paso "Arrancando el servicio"
Detalle "Para detenerlo: Ctrl+C en esta ventana."
AbrirCuandoResponda "$urlBase/swagger-ui.html" "$urlBase/actuator/health"

$env:PORT = "$Puerto"
& $mvnw -Plocal spring-boot:run

Write-Host ""
Detalle "Servicio detenido."
Get-Job -ErrorAction SilentlyContinue | Remove-Job -Force -ErrorAction SilentlyContinue
