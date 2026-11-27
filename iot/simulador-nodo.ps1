<#
.SYNOPSIS
  Simulador nativo en PowerShell de un nodo ESP32 para AgroPiura IoT (A2).

.EXAMPLE
  .\iot\simulador-nodo.ps1 -Token "ap_1_TU_TOKEN" -Intervalo 5 -Ciclos 3
  .\iot\simulador-nodo.ps1 -Token "ap_1_TU_TOKEN" -Intervalo 5 -Ciclos 3 -Anomalia
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [string]$Url = "http://localhost:8080/api/telemetria",
    [int]$Intervalo = 10,
    [int]$Ciclos = 0,
    [switch]$Anomalia
)

$rnd = New-Object System.Random
$ronda = 0

while ($Ciclos -eq 0 -or $ronda -lt $Ciclos) {
    $ronda++
    if ($Anomalia) {
        $humSuelo = [math]::Round(8 + $rnd.NextDouble() * 10, 2)
        $tempAmb  = [math]::Round(37.5 + $rnd.NextDouble() * 5.5, 2)
        $humAmb   = [math]::Round(20 + $rnd.NextDouble() * 15, 2)
        $phSuelo  = [math]::Round(4.2 + $rnd.NextDouble() * 0.7, 2)
    } else {
        $humSuelo = [math]::Round(42 + $rnd.NextDouble() * 26, 2)
        $tempAmb  = [math]::Round(22 + $rnd.NextDouble() * 9, 2)
        $humAmb   = [math]::Round(50 + $rnd.NextDouble() * 25, 2)
        $phSuelo  = [math]::Round(6.0 + $rnd.NextDouble() * 1.2, 2)
    }

    $body = @{
        lecturas = @(
            @{ tipoSensor = "humedad_suelo"; valor = $humSuelo },
            @{ tipoSensor = "temperatura_ambiente"; valor = $tempAmb },
            @{ tipoSensor = "humedad_ambiente"; valor = $humAmb },
            @{ tipoSensor = "ph_suelo"; valor = $phSuelo }
        )
    } | ConvertTo-Json -Depth 3

    $hora = Get-Date -Format "HH:mm:ss"
    try {
        $resp = Invoke-RestMethod -Uri $Url -Method Post `
            -Headers @{ Authorization = "Bearer $Token" } `
            -ContentType "application/json" `
            -Body $body
        Write-Host "[$hora] Ronda $ronda -> OK: $($resp | ConvertTo-Json -Compress)"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        Write-Host "[$hora] Ronda $ronda -> HTTP $status : $($_.ErrorDetails.Message)"
    }

    if ($Ciclos -eq 0 -or $ronda -lt $Ciclos) {
        Start-Sleep -Seconds ([math]::Max(1, $Intervalo))
    }
}
