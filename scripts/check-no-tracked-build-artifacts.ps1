$trackedTargets = @(
    git ls-files |
    Where-Object { $_ -match '(^|/)target/' }
)

if ($trackedTargets.Count -gt 0) {
    Write-Error "Tracked Maven target artifacts detected:"
    $trackedTargets | ForEach-Object { Write-Error $_ }
    exit 1
}
