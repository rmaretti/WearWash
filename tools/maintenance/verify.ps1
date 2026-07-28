[CmdletBinding()]
param(
    [string]$RunId = ("run-{0}-{1}" -f `
        (Get-Date -Format "yyyyMMdd-HHmmss"), `
        ([Guid]::NewGuid().ToString("N").Substring(0, 8))),
    [string]$ArtifactsRoot,
    [switch]$ForceExecution
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if ([string]::IsNullOrWhiteSpace($ArtifactsRoot)) {
    $ArtifactsRoot = Join-Path $repositoryRoot "build\maintenance-agent"
}
$runDirectory = Join-Path $ArtifactsRoot $RunId
$commandDirectory = Join-Path $runDirectory "commands"
$artifactDirectory = Join-Path $runDirectory "artifacts"
New-Item -ItemType Directory -Force -Path $commandDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $artifactDirectory | Out-Null

$stages = @(
    [pscustomobject]@{
        id = "local-tests"
        arguments = @("testDebugUnitTest", "--console=plain", "--no-daemon")
        timeoutSeconds = 600
    },
    [pscustomobject]@{
        id = "lint-build"
        arguments = @("lintDebug", "assembleDebug", "--console=plain", "--no-daemon")
        timeoutSeconds = 600
    },
    [pscustomobject]@{
        id = "device-e2e"
        arguments = @(
            "pixelApi34DebugAndroidTest",
            "--console=plain",
            "--no-daemon",
            "-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect"
        )
        timeoutSeconds = 1200
    }
)

function Write-JsonFile {
    param(
        [Parameter(Mandatory = $true)]$Value,
        [Parameter(Mandatory = $true)][string]$Path
    )

    $Value | ConvertTo-Json -Depth 12 |
        Set-Content -LiteralPath $Path -Encoding UTF8
}

function Copy-ArtifactPath {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][string]$DestinationRoot
    )

    $source = Join-Path $repositoryRoot $RelativePath
    if (-not (Test-Path -LiteralPath $source)) {
        return
    }

    $destination = Join-Path $DestinationRoot $RelativePath
    $parent = Split-Path -Parent $destination
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    Copy-Item -LiteralPath $source -Destination $destination -Recurse -Force
}

function Copy-VerificationArtifacts {
    param([Parameter(Mandatory = $true)][string]$StageId)

    $destination = Join-Path $artifactDirectory $StageId
    New-Item -ItemType Directory -Force -Path $destination | Out-Null

    $paths = switch ($StageId) {
        "local-tests" {
            @(
                "app\build\test-results\testDebugUnitTest",
                "app\build\reports\tests\testDebugUnitTest"
            )
        }
        "lint-build" {
            @(
                "app\build\reports\lint-results-debug.html",
                "app\build\reports\lint-results-debug.xml",
                "app\build\outputs\apk\debug\app-debug.apk"
            )
        }
        "device-e2e" {
            @(
                "app\build\reports\androidTests\managedDevice",
                "app\build\outputs\androidTest-results\managedDevice",
                "app\build\outputs\managed_device_android_test_additional_output",
                "app\build\outputs\apk\debug\app-debug.apk",
                "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
            )
        }
        default {
            throw "Unknown verification stage '$StageId'"
        }
    }

    $paths | ForEach-Object {
        Copy-ArtifactPath -RelativePath $_ -DestinationRoot $destination
    }
}

function Get-ArtifactHashes {
    $files = Get-ChildItem -LiteralPath $artifactDirectory -File -Recurse
    return @(
        $files | ForEach-Object {
            [pscustomobject]@{
                path = $_.FullName.Substring($runDirectory.Length + 1)
                sha256 = (
                    Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
                ).Hash
                size_bytes = $_.Length
            }
        }
    )
}

$run = [ordered]@{
    schema_version = 1
    run_id = $RunId
    started_at = (Get-Date).ToUniversalTime().ToString("o")
    repository = $repositoryRoot
    baseline_commit = (& git -C $repositoryRoot rev-parse HEAD).Trim()
    initial_git_status = @(& git -C $repositoryRoot status --short)
    force_execution = [bool]$ForceExecution
    stages = @()
    result = "running"
}
Write-JsonFile -Value $run -Path (Join-Path $runDirectory "run.json")

foreach ($stage in $stages) {
    $stdoutPath = Join-Path $commandDirectory ("{0}.stdout.log" -f $stage.id)
    $stderrPath = Join-Path $commandDirectory ("{0}.stderr.log" -f $stage.id)
    $stageArguments = @($stage.arguments)
    if ($ForceExecution) {
        if ($stage.id -eq "device-e2e") {
            $stageArguments += (
                "-Pandroid.testInstrumentationRunnerArguments." +
                "maintenanceRunId=$RunId"
            )
        }
        else {
            $stageArguments += "--rerun-tasks"
        }
        $stageArguments += "--max-workers=1"
    }
    $gradleCommand = "gradlew.bat " + ($stageArguments -join " ")
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = Join-Path $env:SystemRoot "System32\cmd.exe"
    $startInfo.Arguments = "/d /s /c `"$gradleCommand`""
    $startInfo.WorkingDirectory = $repositoryRoot
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()

    $completed = $process.WaitForExit($stage.timeoutSeconds * 1000)
    if (-not $completed) {
        & taskkill.exe /PID $process.Id /T /F 2>&1 | Out-Null
        $process.WaitForExit()
        $exitCode = $null
        $result = "timed_out"
    }
    else {
        $process.WaitForExit()
        $process.Refresh()
        $exitCode = $process.ExitCode
        $result = if ($exitCode -eq 0) { "passed" } else { "failed" }
    }
    $stdoutTask.Result | Set-Content -LiteralPath $stdoutPath -Encoding UTF8
    $stderrTask.Result | Set-Content -LiteralPath $stderrPath -Encoding UTF8
    $process.Dispose()
    $stopwatch.Stop()

    Copy-VerificationArtifacts -StageId $stage.id
    $stageResult = [ordered]@{
        id = $stage.id
        command = ".\$gradleCommand"
        timeout_seconds = $stage.timeoutSeconds
        exit_code = $exitCode
        duration_ms = $stopwatch.ElapsedMilliseconds
        result = $result
        stdout = "commands\$($stage.id).stdout.log"
        stderr = "commands\$($stage.id).stderr.log"
    }
    $run.stages += $stageResult
    Write-JsonFile -Value $run -Path (Join-Path $runDirectory "run.json")

    if ($result -ne "passed") {
        $run.result = $result
        break
    }
}

if ($run.result -eq "running") {
    $run.result = "passed"
}
$run.completed_at = (Get-Date).ToUniversalTime().ToString("o")
$run.final_git_status = @(& git -C $repositoryRoot status --short)
$run.artifacts = Get-ArtifactHashes
Write-JsonFile -Value $run -Path (Join-Path $runDirectory "run.json")

Write-Output (
    "Verification {0}. Run artifacts: {1}" -f $run.result, $runDirectory
)
if ($run.result -ne "passed") {
    exit 1
}
