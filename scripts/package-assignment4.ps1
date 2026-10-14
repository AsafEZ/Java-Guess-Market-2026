$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path -Parent $PSScriptRoot
$client = Join-Path $root 'WebClient'
$outputDirectory = Join-Path $root 'target\assignment4'
$readme = Join-Path $outputDirectory 'Guess_Market_Ex4_README.docx'
$archivePath = Join-Path $outputDirectory 'Guess_Market_Ex4_Web_Client.zip'

if (-not (Test-Path -LiteralPath $readme -PathType Leaf)) {
    throw 'Generate the Word README before packaging Assignment 4.'
}

$files = @(
    'index.html'
    'vite.config.js'
    'package.json'
    'package-lock.json'
    'run-web-client.bat'
)
$files += Get-ChildItem -LiteralPath (Join-Path $client 'src') -File |
    ForEach-Object { 'src/' + $_.Name }

$stream = [System.IO.File]::Open($archivePath, [System.IO.FileMode]::Create)
try {
    $archive = [System.IO.Compression.ZipArchive]::new(
        $stream, [System.IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        foreach ($relative in $files) {
            $source = Join-Path $client ($relative.Replace('/', '\'))
            if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
                throw "Missing Web client file: $relative"
            }
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive, $source, "WebClient/$relative",
                [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archive, $readme, 'README.docx',
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    } finally {
        $archive.Dispose()
    }
} finally {
    $stream.Dispose()
}

$check = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
try {
    $names = @($check.Entries | ForEach-Object { $_.FullName })
    foreach ($required in @('README.docx', 'WebClient/run-web-client.bat',
            'WebClient/package-lock.json', 'WebClient/src/App.jsx')) {
        if ($names -notcontains $required) { throw "Missing archive entry: $required" }
    }
    if (@($names | Where-Object { $_ -match 'node_modules|/dist/|target/' }).Count) {
        throw 'The archive contains a generated or private path.'
    }
    if (@($names | Where-Object { $_ -like '*.docx' }).Count -ne 1) {
        throw 'The archive must contain exactly one Word README.'
    }
    Write-Output "Created $archivePath with $($names.Count) files."
} finally {
    $check.Dispose()
}
