param(
    [string]$Tag,
    [string]$Repo = "jmoalves/levain-2",
    [string]$InstallDir = "$HOME\levain",
    [switch]$Jar,
    [switch]$Binary,
    [switch]$AddToPath
)

$ErrorActionPreference = "Stop"

if ($Jar -and $Binary) {
    throw "Choose either -Jar or -Binary, not both."
}

$mode = if ($Jar) { "jar" } else { "binary" }

function Get-LatestNightlyTag {
    param([string]$RepoName)

    $uri = "https://api.github.com/repos/$RepoName/releases?per_page=100"
    $headers = @{ "User-Agent" = "levain-install" }
    $releases = Invoke-RestMethod -Uri $uri -Headers $headers

    $nightlies = $releases | Where-Object { $_.prerelease -and $_.tag_name -like "nightly-*" }
    if (-not $nightlies) {
        throw "No nightly releases found for $RepoName"
    }

    return ($nightlies | Sort-Object published_at | Select-Object -Last 1).tag_name
}

if (-not $Tag) {
    $Tag = Get-LatestNightlyTag -RepoName $Repo
}

$asset = if ($mode -eq "jar") { "levain.jar" } else { "levain.exe" }
$downloadUrl = "https://github.com/$Repo/releases/download/$Tag/$asset"

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
$destination = Join-Path $InstallDir $asset

Write-Output "Downloading $asset from $downloadUrl"
Invoke-WebRequest -Uri $downloadUrl -OutFile $destination -UseBasicParsing

if ($mode -eq "jar") {
    $cmdPath = Join-Path $InstallDir "levain.cmd"
    @"
@echo off
java -jar "%~dp0levain.jar" %*
"@ | Set-Content -Path $cmdPath -Encoding ASCII
}

if ($AddToPath) {
    if ($env:GITHUB_PATH) {
        Add-Content $env:GITHUB_PATH $InstallDir
    }
    $env:PATH = "$InstallDir;$env:PATH"
}

Write-Output "Installed Levain ($mode) to $InstallDir using tag $Tag"
