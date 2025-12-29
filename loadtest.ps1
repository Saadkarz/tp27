# ============================================
# TP27 - Test de Concurrence, Verrous DB & Résilience
# Script de test de charge PowerShell
# ============================================
#
# Usage: .\loadtest.ps1 -BookId <ID> -Requests <N>
# Exemple: .\loadtest.ps1 -BookId 1 -Requests 50
#
# Ce script:
# - Lance N requêtes POST /api/books/{id}/borrow en parallèle
# - Répartit les requêtes sur 3 instances (8081, 8083, 8084)
# - Compte les succès (200), conflits (409) et erreurs
#

param(
    [int]$BookId = 1,
    [int]$Requests = 50
)

$Ports = @(8081, 8083, 8084)

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  LOAD TEST - TP27 Concurrence" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "BookId   : $BookId" -ForegroundColor Yellow
Write-Host "Requests : $Requests" -ForegroundColor Yellow
Write-Host "Ports    : $($Ports -join ', ')" -ForegroundColor Yellow
Write-Host ""
Write-Host "Lancement des requetes en parallele..." -ForegroundColor Gray
Write-Host ""

$jobs = @()

for ($i = 1; $i -le $Requests; $i++) {
    $port = $Ports[$i % 3]
    $url = "http://localhost:$port/api/books/$BookId/borrow"

    $jobs += Start-Job -ScriptBlock {
        param($u, $p, $idx)
        try {
            $resp = Invoke-WebRequest -Uri $u -Method POST -UseBasicParsing -TimeoutSec 30
            [PSCustomObject]@{ 
                Index = $idx
                Port = $p
                Status = $resp.StatusCode
                Body = $resp.Content 
            }
        } catch {
            if ($_.Exception.Response -ne $null) {
                $status = [int]$_.Exception.Response.StatusCode
                $body = ""
                try {
                    $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
                    $body = $reader.ReadToEnd()
                    $reader.Close()
                } catch {}
                [PSCustomObject]@{ 
                    Index = $idx
                    Port = $p
                    Status = $status
                    Body = $body 
                }
            } else {
                [PSCustomObject]@{ 
                    Index = $idx
                    Port = $p
                    Status = -1
                    Body = $_.Exception.Message 
                }
            }
        }
    } -ArgumentList $url, $port, $i
}

Write-Host "En attente de $($jobs.Count) jobs..." -ForegroundColor Gray

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force

# Comptage des résultats
$successList = @($results | Where-Object { $_.Status -eq 200 })
$conflictList = @($results | Where-Object { $_.Status -eq 409 })
$otherList = @($results | Where-Object { $_.Status -ne 200 -and $_.Status -ne 409 })

$success = $successList.Count
$conflict = $conflictList.Count
$other = $otherList.Count

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  RESULTATS" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Success (200)  : $success" -ForegroundColor Green
Write-Host "Conflict (409) : $conflict" -ForegroundColor Yellow
Write-Host "Other          : $other" -ForegroundColor Red
Write-Host ""

# Afficher les détails des succès (optionnel)
if ($success -gt 0) {
    Write-Host "--- Derniers succes ---" -ForegroundColor Green
    $successList | Select-Object -Last 3 | ForEach-Object {
        Write-Host "  Port $($_.Port): $($_.Body)" -ForegroundColor DarkGreen
    }
    Write-Host ""
}

# Afficher les erreurs s'il y en a
if ($other -gt 0) {
    Write-Host "--- Erreurs (Other) ---" -ForegroundColor Red
    $otherList | ForEach-Object {
        Write-Host "  Port $($_.Port), Status $($_.Status): $($_.Body)" -ForegroundColor DarkRed
    }
    Write-Host ""
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
