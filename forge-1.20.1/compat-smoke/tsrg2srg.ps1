param(
    [Parameter(Mandatory = $true)][string]$Source,
    [Parameter(Mandatory = $true)][string]$Output
)

# Test-only conversion for Mixin's development refmap remapper.  ForgeGradle's
# MCP config stores the exact same searge-to-official relation in TSRG2.
$currentClass = $null
$currentMappedClass = $null
$out = [System.Collections.Generic.List[string]]::new()
foreach ($line in Get-Content -LiteralPath $Source) {
    if ($line.Length -eq 0 -or $line.StartsWith('#') -or $line.StartsWith('tsrg2 ')) { continue }
    if (-not $line.StartsWith("`t")) {
        $parts = $line -split '\s+'
        if ($parts.Count -ge 2) {
            $currentClass = $parts[0]
            $currentMappedClass = $parts[1]
            $out.Add("CL: $($parts[0]) $($parts[1])")
        }
        continue
    }
    if ($line.StartsWith("`t`t") -or $null -eq $currentClass) { continue }
    $parts = $line.Trim() -split '\s+'
    if ($parts.Count -eq 2) {
        $out.Add("FD: $currentClass/$($parts[0]) $currentMappedClass/$($parts[1])")
    } elseif ($parts.Count -ge 3 -and $parts[1].StartsWith('(')) {
        $out.Add("MD: $currentClass/$($parts[0]) $($parts[1]) $currentMappedClass/$($parts[2]) $($parts[1])")
    }
}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Output) | Out-Null
[System.IO.File]::WriteAllLines($Output, $out)
