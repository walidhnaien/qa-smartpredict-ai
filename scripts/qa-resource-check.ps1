$ErrorActionPreference = 'Stop'
$os = Get-CimInstance Win32_OperatingSystem
$disk = Get-PSDrive -Name C
$free = [math]::Round($os.FreePhysicalMemory / 1024, 1)
$warnings = @()
if ($free -lt 1024) { $warnings += 'WINDOWS_MEMORY_CRITICAL' }
elseif ($free -lt 3072) { $warnings += 'WINDOWS_MEMORY_LOW' }
if ($disk.Free -lt 30GB) { $warnings += 'WINDOWS_DISK_LOW' }
$backend = $null
try { $backend = Invoke-RestMethod -Uri 'http://localhost:8081/api/ai/orchestrator/preflight' -TimeoutSec 15 }
catch { $warnings += 'BACKEND_PREFLIGHT_UNAVAILABLE' }
[ordered]@{
  timestamp = (Get-Date).ToUniversalTime().ToString('o')
  windowsAvailableMemoryMb = $free
  windowsAvailableVirtualMemoryMb = [math]::Round($os.FreeVirtualMemory / 1024, 1)
  windowsTotalMemoryMb = [math]::Round($os.TotalVisibleMemorySize / 1024, 1)
  windowsDiskCFreeGiB = [math]::Round($disk.Free / 1GB, 2)
  hostWarnings = $warnings
  backend = $backend
} | ConvertTo-Json -Depth 12
