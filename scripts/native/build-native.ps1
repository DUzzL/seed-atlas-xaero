[CmdletBinding()]
param(
    [ValidateSet('windows-x86_64', 'linux-x86_64', 'macos-x86_64', 'macos-aarch64')]
    [string]$Platform,
    [ValidateSet('Release', 'RelWithDebInfo', 'Debug')]
    [string]$Configuration = 'Release',
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectDirectory = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..\..'))
$nativeSource = Join-Path $projectDirectory 'src\main\native'

if (-not $Platform) {
    $architecture = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
    $runtimeInformation = [System.Runtime.InteropServices.RuntimeInformation]
    $osPlatform = [System.Runtime.InteropServices.OSPlatform]
    if ($runtimeInformation::IsOSPlatform($osPlatform::Windows) -and $architecture -eq 'X64') {
        $Platform = 'windows-x86_64'
    } elseif ($runtimeInformation::IsOSPlatform($osPlatform::Linux) -and $architecture -eq 'X64') {
        $Platform = 'linux-x86_64'
    } elseif ($runtimeInformation::IsOSPlatform($osPlatform::OSX) -and $architecture -eq 'X64') {
        $Platform = 'macos-x86_64'
    } elseif ($runtimeInformation::IsOSPlatform($osPlatform::OSX) -and $architecture -eq 'Arm64') {
        $Platform = 'macos-aarch64'
    } else {
        throw "Unsupported host platform: $([System.Runtime.InteropServices.RuntimeInformation]::OSDescription) / $architecture"
    }
}

$buildDirectory = Join-Path $projectDirectory "build\native\$Platform"
$resourceRoot = Join-Path $projectDirectory "build\generated\native-resources\natives\$Platform"

cmake -S $nativeSource -B $buildDirectory `
    "-DCMAKE_BUILD_TYPE=$Configuration" `
    "-DCMAKE_INSTALL_PREFIX=$resourceRoot" `
    "-DBUILD_TESTING=$($RunTests.IsPresent.ToString().ToUpperInvariant())"
if ($LASTEXITCODE -ne 0) { throw 'CMake configure failed' }

cmake --build $buildDirectory --config $Configuration --parallel
if ($LASTEXITCODE -ne 0) { throw 'Native build failed' }

if ($RunTests) {
    ctest --test-dir $buildDirectory -C $Configuration --output-on-failure
    if ($LASTEXITCODE -ne 0) { throw 'Native tests failed' }
}

cmake --install $buildDirectory --config $Configuration
if ($LASTEXITCODE -ne 0) { throw 'Native install failed' }

Write-Host "Native resource generated at $resourceRoot"
