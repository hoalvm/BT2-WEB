[CmdletBinding()]
param(
    [string]$TomcatServiceName = "Tomcat10",
    [string]$UploadDirectory = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$applicationUrl = "http://localhost:8080/jpa-web-assignment-01/"
$warName = "jpa-web-assignment-01.war"
$databasePassword = "123456"

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Invoke-ElevatedSelf {
    $escapedScriptPath = $PSCommandPath.Replace("'", "''")
    $command = "& '$escapedScriptPath'"
    $encodedCommand = [Convert]::ToBase64String(
        [Text.Encoding]::Unicode.GetBytes($command)
    )
    $process = Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand) `
        -Verb RunAs `
        -Wait `
        -PassThru
    exit $process.ExitCode
}

function Get-EnvironmentDirectory([string]$variableName, [bool]$required) {
    $value = [Environment]::GetEnvironmentVariable($variableName, "Machine")
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($variableName, "User")
    }
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($variableName, "Process")
    }

    if ([string]::IsNullOrWhiteSpace($value)) {
        if ($required) {
            throw "Environment variable $variableName is not configured."
        }
        return $null
    }

    return [System.IO.Path]::GetFullPath($value.Trim())
}

function Test-JavaInstallation([string]$javaHome) {
    $javaExecutable = Join-Path $javaHome "bin\java.exe"
    $javacExecutable = Join-Path $javaHome "bin\javac.exe"
    $releaseFile = Join-Path $javaHome "release"

    if (-not (Test-Path -LiteralPath $javaExecutable -PathType Leaf) -or
        -not (Test-Path -LiteralPath $javacExecutable -PathType Leaf) -or
        -not (Test-Path -LiteralPath $releaseFile -PathType Leaf)) {
        throw "JAVA_HOME does not point to a valid JDK: $javaHome"
    }

    $versionLine = Get-Content -LiteralPath $releaseFile | Where-Object { $_ -like "JAVA_VERSION=*" } | Select-Object -First 1
    if (-not $versionLine -or $versionLine -notmatch 'JAVA_VERSION="([^"]+)"') {
        throw "Unable to read the Java version from $releaseFile"
    }

    $version = $Matches[1]
    $versionParts = $version.Split('.')
    $majorVersion = if ($versionParts[0] -eq "1") { [int]$versionParts[1] } else { [int]$versionParts[0] }
    if ($majorVersion -lt 24) {
        throw "This project requires JDK 24 or newer, but JAVA_HOME points to Java $version."
    }

    $env:JAVA_HOME = $javaHome
    $env:Path = (Join-Path $javaHome "bin") + ";" + $env:Path
    return $version
}

function Resolve-TomcatHome {
    $resolvedCandidate = Get-EnvironmentDirectory "CATALINA_HOME" $true
    if ((Test-Path -LiteralPath (Join-Path $resolvedCandidate "bin\catalina.bat") -PathType Leaf) -and
        (Test-Path -LiteralPath (Join-Path $resolvedCandidate "webapps") -PathType Container)) {
        return $resolvedCandidate
    }

    throw "CATALINA_HOME does not point to a valid Tomcat installation: $resolvedCandidate"
}

function Resolve-MySqlClient {
    $command = Get-Command "mysql.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $mysqlRoot = "C:\Program Files\MySQL"
    if (Test-Path -LiteralPath $mysqlRoot -PathType Container) {
        $client = Get-ChildItem -LiteralPath $mysqlRoot -Filter "mysql.exe" -File -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($client) {
            return $client.FullName
        }
    }

    throw "mysql.exe was not found. Install MySQL Server 8.x first."
}

function Initialize-Database([string]$mysqlClient) {
    $sqlFile = Join-Path $projectRoot "database.sql"
    $sql = Get-Content -LiteralPath $sqlFile -Raw -Encoding UTF8

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $mysqlClient
    $startInfo.Arguments = "--protocol=TCP --host=localhost --port=3306 --user=root --password=$databasePassword --default-character-set=utf8mb4"
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true

    if ($startInfo.PSObject.Properties.Name -contains "StandardInputEncoding") {
        $startInfo.StandardInputEncoding = New-Object System.Text.UTF8Encoding($false)
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $process.StandardInput.Write($sql)
    $process.StandardInput.Close()
    $standardOutput = $process.StandardOutput.ReadToEnd()
    $standardError = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "Database initialization failed. MySQL returned: $standardError"
    }

    if ($standardOutput) {
        Write-Host $standardOutput
    }
}

if (-not (Test-Administrator)) {
    Invoke-ElevatedSelf
}

foreach ($requiredCommand in @("git.exe", "mysql.exe")) {
    if (-not (Get-Command $requiredCommand -ErrorAction SilentlyContinue)) {
        throw "$requiredCommand was not found in PATH. Install it and open a new terminal."
    }
}

$javaHome = Get-EnvironmentDirectory "JAVA_HOME" $true
$javaVersion = Test-JavaInstallation $javaHome
$tomcatHome = Resolve-TomcatHome
$catalinaBase = Get-EnvironmentDirectory "CATALINA_BASE" $false
if (-not $catalinaBase) {
    $catalinaBase = $tomcatHome
}
if (-not (Test-Path -LiteralPath (Join-Path $catalinaBase "webapps") -PathType Container)) {
    throw "CATALINA_BASE does not contain a webapps directory: $catalinaBase"
}

$tomcatService = Get-Service -Name $TomcatServiceName -ErrorAction SilentlyContinue
if (-not $tomcatService) {
    throw "Windows service '$TomcatServiceName' was not found. Install Tomcat 10.1 with the Windows Service Installer."
}
$serviceFilterName = $TomcatServiceName.Replace("'", "''")
$tomcatServiceInfo = Get-CimInstance -ClassName Win32_Service -Filter "Name='$serviceFilterName'"
if (-not $tomcatServiceInfo) {
    throw "Unable to read the configuration of Windows service '$TomcatServiceName'."
}

$mysqlServices = @(Get-Service | Where-Object { $_.Name -like "MySQL*" })
if ($mysqlServices.Count -eq 0) {
    throw "No MySQL Windows service was found."
}
$runningMySqlService = $mysqlServices | Where-Object { $_.Status -eq "Running" } | Select-Object -First 1
if (-not $runningMySqlService) {
    $runningMySqlService = $mysqlServices | Select-Object -First 1
    Start-Service -Name $runningMySqlService.Name
    (Get-Service -Name $runningMySqlService.Name).WaitForStatus("Running", [TimeSpan]::FromSeconds(30))
}

if ([string]::IsNullOrWhiteSpace($UploadDirectory)) {
    $UploadDirectory = Get-EnvironmentDirectory "JPAWEB_UPLOAD_DIR" $false
}
if ([string]::IsNullOrWhiteSpace($UploadDirectory)) {
    $UploadDirectory = "C:\ProgramData\JPAWeb\uploads"
}
$UploadDirectory = [System.IO.Path]::GetFullPath($UploadDirectory)

[Environment]::SetEnvironmentVariable("JPAWEB_UPLOAD_DIR", $UploadDirectory, "Machine")
$env:JAVA_HOME = $javaHome
$env:CATALINA_HOME = $tomcatHome
$env:CATALINA_BASE = $catalinaBase
$env:JPAWEB_UPLOAD_DIR = $UploadDirectory

New-Item -ItemType Directory -Force -Path $UploadDirectory | Out-Null
$serviceAccount = $tomcatServiceInfo.StartName
$aclIdentity = switch -Regex ($serviceAccount) {
    '^(LocalSystem|NT AUTHORITY\\SYSTEM)$' { $null; break }
    '^(NT AUTHORITY\\)?LocalService$' { "*S-1-5-19"; break }
    '^(NT AUTHORITY\\)?NetworkService$' { "*S-1-5-20"; break }
    default { $serviceAccount }
}
if ($aclIdentity) {
    & icacls.exe $UploadDirectory /grant "${aclIdentity}:(OI)(CI)M" /T /C | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to grant upload-directory write access to Tomcat account '$serviceAccount'."
    }
}

$mailUsername = [Environment]::GetEnvironmentVariable("JPAWEB_MAIL_USERNAME", "Machine")
$mailAppPassword = [Environment]::GetEnvironmentVariable("JPAWEB_MAIL_APP_PASSWORD", "Machine")
if ([string]::IsNullOrWhiteSpace($mailUsername) -or [string]::IsNullOrWhiteSpace($mailAppPassword)) {
    Write-Warning "Gmail environment variables were not detected. Set JPAWEB_MAIL_USERNAME and JPAWEB_MAIL_APP_PASSWORD at Machine scope, or configure the documented Tomcat JVM properties, before testing OTP email."
} else {
    Write-Host "Gmail OTP configuration detected for the Tomcat service." -ForegroundColor Green
}

Write-Host "Preflight passed: Git, Java $javaVersion, MySQL, Tomcat, and environment variables are valid." -ForegroundColor Green

Write-Host "[1/4] Initializing the jpa_web database..."
Initialize-Database (Resolve-MySqlClient)

Write-Host "[2/4] Building the WAR with Maven Wrapper..."
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot "mvnw.cmd") clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }
} finally {
    Pop-Location
}

$webappsRoot = [System.IO.Path]::GetFullPath((Join-Path $catalinaBase "webapps"))
$deployedWar = [System.IO.Path]::GetFullPath((Join-Path $webappsRoot $warName))
$explodedApplication = [System.IO.Path]::GetFullPath((Join-Path $webappsRoot "jpa-web-assignment-01"))
$expectedPrefix = $webappsRoot.TrimEnd('\') + '\'

if (-not $deployedWar.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase) -or
    -not $explodedApplication.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "The resolved Tomcat deployment path is unsafe."
}

Write-Host "[3/4] Deploying the WAR and starting Tomcat..."
if ((Get-Service -Name $TomcatServiceName).Status -ne "Stopped") {
    Stop-Service -Name $TomcatServiceName -Force
    (Get-Service -Name $TomcatServiceName).WaitForStatus("Stopped", [TimeSpan]::FromSeconds(30))
}

if (Test-Path -LiteralPath $explodedApplication) {
    Remove-Item -LiteralPath $explodedApplication -Recurse -Force
}
if (Test-Path -LiteralPath $deployedWar) {
    Remove-Item -LiteralPath $deployedWar -Force
}

Copy-Item -LiteralPath (Join-Path $projectRoot "target\$warName") -Destination $deployedWar -Force
Start-Service -Name $TomcatServiceName
(Get-Service -Name $TomcatServiceName).WaitForStatus("Running", [TimeSpan]::FromSeconds(30))

Write-Host "[4/4] Checking the application..."
$deadline = (Get-Date).AddSeconds(60)
do {
    try {
        $response = Invoke-WebRequest -Uri $applicationUrl -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            Write-Host "SUCCESS: $applicationUrl" -ForegroundColor Green
            Start-Process $applicationUrl
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 2
    }
} while ((Get-Date) -lt $deadline)

throw "Tomcat is running, but the application did not return HTTP 200 within 60 seconds."
