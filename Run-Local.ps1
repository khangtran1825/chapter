param([switch]$PrepareOnly)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$envPath = Join-Path $PSScriptRoot '.env'
$settings = [ordered]@{}
if (Test-Path -LiteralPath $envPath) {
    foreach ($line in [IO.File]::ReadAllLines($envPath)) {
        if ($line -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $settings[$matches[1]] = $matches[2] }
    }
}
function New-LocalSecret {
    $bytes = New-Object byte[] 24
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
if (-not $settings['POSTGRES_DB']) { $settings['POSTGRES_DB'] = 'chapter' }
if (-not $settings['POSTGRES_USER']) { $settings['POSTGRES_USER'] = 'chapter' }
if (-not $settings['POSTGRES_PORT']) { $settings['POSTGRES_PORT'] = '15432' }
if (-not $settings['POSTGRES_PASSWORD'] -or $settings['POSTGRES_PASSWORD'] -eq 'replace-with-a-random-local-password') { $settings['POSTGRES_PASSWORD'] = New-LocalSecret }
if (-not $settings['CHAPTER_ADMIN_USERNAME']) { $settings['CHAPTER_ADMIN_USERNAME'] = 'chapteradmin' }
if (-not $settings['CHAPTER_ADMIN_PASSWORD']) { $settings['CHAPTER_ADMIN_PASSWORD'] = New-LocalSecret }
$lines = foreach ($entry in $settings.GetEnumerator()) { $entry.Key + '=' + $entry.Value }
[IO.File]::WriteAllLines($envPath, [string[]]$lines, (New-Object Text.UTF8Encoding($false)))
docker compose up -d --wait db
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. Start Docker Desktop and retry.' }
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:' + $settings['POSTGRES_PORT'] + '/' + [Uri]::EscapeDataString($settings['POSTGRES_DB'])
$env:SPRING_DATASOURCE_USERNAME = $settings['POSTGRES_USER']
$env:SPRING_DATASOURCE_PASSWORD = $settings['POSTGRES_PASSWORD']
$env:CHAPTER_ADMIN_USERNAME = $settings['CHAPTER_ADMIN_USERNAME']
$env:CHAPTER_ADMIN_PASSWORD = $settings['CHAPTER_ADMIN_PASSWORD']
Write-Output 'Local configuration ready. Administrator credentials are in ignored .env (not printed).'
if (-not $PrepareOnly) {
    & (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
    exit $LASTEXITCODE
}
