param(
    [string]$Maven = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$target = [IO.Path]::GetFullPath((Join-Path $repo 'target\assignment3-delivery'))
if (-not $target.StartsWith($repo + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Delivery target must remain inside this workspace.'
}
if (-not (Test-Path -LiteralPath $Maven)) {
    throw "Maven not found: $Maven"
}

function Invoke-Build([string]$module, [string]$goal) {
    & $Maven -q -f (Join-Path $repo "$module\pom.xml") `
        "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" $goal
    if ($LASTEXITCODE -ne 0) {
        throw "$module Maven $goal failed."
    }
}

Invoke-Build 'Protocol' 'install'
Invoke-Build 'Engine' 'install'
Invoke-Build 'Server' 'package'
Invoke-Build 'Client' 'package'

$dependencies = Join-Path $repo 'Client\target\assignment3-lib'
New-Item -ItemType Directory -Force -Path $dependencies | Out-Null
& $Maven -q -f (Join-Path $repo 'Client\pom.xml') `
    "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" `
    'org.apache.maven.plugins:maven-dependency-plugin:3.7.0:copy-dependencies' `
    '-DincludeScope=runtime' "-DoutputDirectory=$dependencies"
if ($LASTEXITCODE -ne 0) {
    throw 'Client dependency copy failed.'
}

if (Test-Path -LiteralPath $target) {
    Remove-Item -LiteralPath $target -Recurse -Force
}
$client = Join-Path $target 'Client'
$lib = Join-Path $client 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null
Copy-Item -LiteralPath (Join-Path $repo 'Server\target\Server-1.0-SNAPSHOT.war') `
    -Destination (Join-Path $target 'Server.war')
Copy-Item -LiteralPath (Join-Path $repo 'Client\target\Client-1.0-SNAPSHOT.jar') `
    -Destination (Join-Path $client 'Client.jar')
Copy-Item -LiteralPath (Join-Path $repo 'Client\run-client.bat') -Destination $client
Copy-Item -Path (Join-Path $dependencies '*.jar') -Destination $lib

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Add-ZipText($archive, [string]$path, [string]$value) {
    $entry = $archive.CreateEntry($path)
    $writer = [IO.StreamWriter]::new($entry.Open(), [Text.UTF8Encoding]::new($false))
    try { $writer.Write($value) } finally { $writer.Dispose() }
}
function Escape-Xml([string]$value) {
    return [System.Security.SecurityElement]::Escape($value)
}

$submitterFile = Join-Path $repo 'docs\Assignment_3_Submitter.local.json'
if (-not (Test-Path -LiteralPath $submitterFile)) {
    throw 'Create docs/Assignment_3_Submitter.local.json before packaging the hand-in.'
}
$submitter = Get-Content -LiteralPath $submitterFile -Raw -Encoding UTF8 | ConvertFrom-Json
if ([string]::IsNullOrWhiteSpace($submitter.name) -or
        $submitter.id -notmatch '^\d{9}$' -or
        [string]::IsNullOrWhiteSpace($submitter.email)) {
    throw 'Submitter name, nine-digit ID, and email are required.'
}
if ($submitter.solo) {
    $partner = 'Partner: none (solo submission)'
} elseif (-not [string]::IsNullOrWhiteSpace($submitter.partnerName) -and
        $submitter.partnerId -match '^\d{9}$' -and
        -not [string]::IsNullOrWhiteSpace($submitter.partnerEmail)) {
    $partner = "Partner: $($submitter.partnerName), $($submitter.partnerId), $($submitter.partnerEmail)"
} else {
    throw 'Partner details are required for a joint submission.'
}
$readmeSource = Get-Content -LiteralPath (Join-Path $repo 'docs\Assignment_3_Readme.md') `
    -Raw -Encoding UTF8
$readmeSource = $readmeSource.Replace('[ADD FULL NAME]', $submitter.name)
$readmeSource = $readmeSource.Replace('[ADD ID NUMBER]', $submitter.id)
$readmeSource = $readmeSource.Replace('[ADD CONTACT EMAIL]', $submitter.email)
$readmeSource = $readmeSource.Replace('Partner (if applicable): [ADD NAME, ID AND EMAIL]',
    $partner)
if ($readmeSource.Contains('[ADD ')) {
    throw 'The hand-in README still contains an unfilled submitter field.'
}
$readme = $readmeSource -split '\r?\n'
$paragraphs = [Text.StringBuilder]::new()
foreach ($line in $readme) {
    $style = ''
    if ($line.StartsWith('# ')) {
        $style = '<w:pPr><w:pStyle w:val="Heading1"/></w:pPr>'
        $line = $line.Substring(2)
    } elseif ($line.StartsWith('## ')) {
        $style = '<w:pPr><w:pStyle w:val="Heading2"/></w:pPr>'
        $line = $line.Substring(3)
    }
    [void]$paragraphs.Append('<w:p>').Append($style).Append('<w:r><w:t xml:space="preserve">')
    [void]$paragraphs.Append((Escape-Xml $line)).Append('</w:t></w:r></w:p>')
}
$docx = Join-Path $target 'README.docx'
$archive = [IO.Compression.ZipFile]::Open($docx, [IO.Compression.ZipArchiveMode]::Create)
try {
    Add-ZipText $archive '[Content_Types].xml' '<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>'
    Add-ZipText $archive '_rels/.rels' '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>'
    Add-ZipText $archive 'word/document.xml' ('<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>' + $paragraphs.ToString() + '<w:sectPr/></w:body></w:document>')
} finally {
    $archive.Dispose()
}

$warFiles = @(Get-ChildItem -LiteralPath $target -Recurse -Filter '*.war')
if ($warFiles.Count -ne 1) {
    throw "Expected exactly one WAR, found $($warFiles.Count)."
}
$zip = Join-Path $repo 'target\Assignment3.zip'
if (Test-Path -LiteralPath $zip) {
    Remove-Item -LiteralPath $zip -Force
}
$submission = [IO.Compression.ZipFile]::Open($zip, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in Get-ChildItem -LiteralPath $target -Recurse -File) {
        $name = $file.FullName.Substring($target.Length + 1).Replace('\', '/')
        $entry = $submission.CreateEntry($name, [IO.Compression.CompressionLevel]::Optimal)
        $source = [IO.File]::OpenRead($file.FullName)
        $destination = $entry.Open()
        try { $source.CopyTo($destination) } finally {
            $destination.Dispose()
            $source.Dispose()
        }
    }
} finally {
    $submission.Dispose()
}
Write-Output $zip
