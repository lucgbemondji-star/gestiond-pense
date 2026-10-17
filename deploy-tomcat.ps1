$ErrorActionPreference = 'Stop'

$dossierProjet = Split-Path -Parent $MyInvocation.MyCommand.Path
$dossierTomcat = 'C:\apache-tomcat-11.0.27'
$maven = Join-Path $dossierProjet '.tools\apache-maven-3.9.16\bin\mvn.cmd'
$fichierConfiguration = Join-Path $dossierProjet 'config.properties'
$urlApplication = 'https://localhost:8443/gestionnaire-depenses-1.0.0-SNAPSHOT/'
$urlHttpApplication = 'http://localhost:8080/gestionnaire-depenses-1.0.0-SNAPSHOT/'
$deploiementEchoue = $false

$identite = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identite)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $arguments
    exit
}

function Configurer-ConnecteurHttpsTomcat {
    $fichierServer = Join-Path $dossierTomcat 'conf\server.xml'
    $fichierKeystore = Join-Path $dossierTomcat 'conf\tomcat-dev-keystore.p12'
    $commandeKeytool = Get-Command keytool.exe -ErrorAction SilentlyContinue
    $cheminKeytool = $null
    if ($commandeKeytool) {
        $cheminKeytool = $commandeKeytool.Source
    } else {
        $commandeJava = Get-Command java.exe -ErrorAction SilentlyContinue
        if ($commandeJava) {
            $cheminKeytool = Join-Path (Split-Path -Parent $commandeJava.Source) 'keytool.exe'
            if (-not (Test-Path -LiteralPath $cheminKeytool)) {
                $cheminKeytool = $null
            }
        }
    }
    if (-not $cheminKeytool) {
        throw 'keytool est introuvable. Installez un JDK et ajoutez son dossier bin au PATH.'
    }

    if (-not (Test-Path -LiteralPath $fichierKeystore)) {
        Write-Host 'Création du certificat auto-signé pour localhost...'
        & $cheminKeytool -genkeypair -alias gestionnaire-depenses-local -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore $fichierKeystore -storepass changeit -keypass changeit -validity 825 -dname 'CN=localhost, OU=Développement, O=Gestionnaire de dépenses, C=FR' -ext 'SAN=dns:localhost,ip:127.0.0.1'
        if ($LASTEXITCODE -ne 0) {
            throw "La création du certificat HTTPS a échoué (code $LASTEXITCODE)."
        }
    }

    $document = New-Object System.Xml.XmlDocument
    $document.Load($fichierServer)
    $service = $document.SelectSingleNode('/Server/Service[@name="Catalina"]')
    if (-not $service) {
        throw "Le service Catalina est absent de $fichierServer."
    }

    $connecteurHttp = $null
    foreach ($connecteur in $service.SelectNodes('Connector')) {
        if ($connecteur.GetAttribute('port') -eq '8080' -and $connecteur.GetAttribute('SSLEnabled') -ne 'true') {
            if ($connecteurHttp) {
                throw "Plusieurs connecteurs HTTP utilisent le port 8080 dans $fichierServer."
            }
            $connecteurHttp = $connecteur
        }
    }
    if (-not $connecteurHttp) {
        throw "Le connecteur HTTP du port 8080 est introuvable dans $fichierServer."
    }

    $connecteurHttps = $null
    foreach ($connecteur in $service.SelectNodes('Connector')) {
        if ($connecteur.GetAttribute('port') -eq '8443') {
            if ($connecteurHttps -or $connecteur.GetAttribute('SSLEnabled') -ne 'true') {
                throw "Le port 8443 est déjà utilisé par un connecteur incompatible dans $fichierServer."
            }
            $connecteurHttps = $connecteur
        }
    }

    $modifications = $false
    if ($connecteurHttp.GetAttribute('redirectPort') -ne '8443') {
        $connecteurHttp.SetAttribute('redirectPort', '8443')
        $modifications = $true
    }

    if (-not $connecteurHttps) {
        $connecteurHttps = $document.CreateElement('Connector')
        $connecteurHttps.SetAttribute('port', '8443')
        $connecteurHttps.SetAttribute('protocol', 'org.apache.coyote.http11.Http11NioProtocol')
        $connecteurHttps.SetAttribute('maxThreads', '150')
        $connecteurHttps.SetAttribute('SSLEnabled', 'true')
        $connecteurHttps.SetAttribute('scheme', 'https')
        $connecteurHttps.SetAttribute('secure', 'true')
        [void]$service.AppendChild($connecteurHttps)
        $configurationSsl = $document.CreateElement('SSLHostConfig')
        [void]$connecteurHttps.AppendChild($configurationSsl)
        $modifications = $true
    } else {
        $configurationSsl = $connecteurHttps.SelectSingleNode('SSLHostConfig')
        if (-not $configurationSsl) {
            $configurationSsl = $document.CreateElement('SSLHostConfig')
            [void]$connecteurHttps.AppendChild($configurationSsl)
            $modifications = $true
        }
    }

    $certificat = $configurationSsl.SelectSingleNode('Certificate')
    if (-not $certificat) {
        $certificat = $document.CreateElement('Certificate')
        [void]$configurationSsl.AppendChild($certificat)
        $modifications = $true
    }
    $attributsCertificat = @{
        certificateKeystoreFile = 'conf/tomcat-dev-keystore.p12'
        certificateKeystorePassword = 'changeit'
        certificateKeystoreType = 'PKCS12'
        type = 'RSA'
    }
    foreach ($nomAttribut in $attributsCertificat.Keys) {
        if ($certificat.GetAttribute($nomAttribut) -ne $attributsCertificat[$nomAttribut]) {
            $certificat.SetAttribute($nomAttribut, $attributsCertificat[$nomAttribut])
            $modifications = $true
        }
    }

    if ($modifications) {
        $horodatage = Get-Date -Format 'yyyyMMdd-HHmmss'
        $sauvegardeServer = "$fichierServer.before-https-$horodatage.bak"
        $fichierTemporaire = "$fichierServer.tmp"
        $parametresXml = New-Object System.Xml.XmlWriterSettings
        $parametresXml.Encoding = New-Object System.Text.UTF8Encoding($false)
        $parametresXml.Indent = $true
        $ecrivainXml = [System.Xml.XmlWriter]::Create($fichierTemporaire, $parametresXml)
        try {
            $document.Save($ecrivainXml)
        } finally {
            $ecrivainXml.Dispose()
        }
        Copy-Item -LiteralPath $fichierServer -Destination $sauvegardeServer
        Move-Item -LiteralPath $fichierTemporaire -Destination $fichierServer -Force
        Write-Host "Configuration Tomcat sauvegardée dans $sauvegardeServer"
    }
}

try {
    if (-not (Test-Path -LiteralPath $maven)) {
        throw "Maven est introuvable : $maven"
    }
    if (-not (Test-Path -LiteralPath $fichierConfiguration)) {
        throw "Le fichier config.properties est introuvable dans le dossier du projet."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $dossierTomcat 'bin\shutdown.bat'))) {
        throw "Tomcat est introuvable dans $dossierTomcat."
    }

    $env:DEPENSES_CONFIG = $fichierConfiguration
    Write-Host 'Compilation et tests en cours...'
    Push-Location $dossierProjet
    try {
        & $maven -q clean verify
        if ($LASTEXITCODE -ne 0) {
            throw "La compilation ou les tests ont échoué (code $LASTEXITCODE)."
        }
    } finally {
        Pop-Location
    }

    Configurer-ConnecteurHttpsTomcat

    $war = Join-Path $dossierProjet 'target\gestionnaire-depenses-1.0.0-SNAPSHOT.war'
    $destinationWar = Join-Path $dossierTomcat 'webapps\gestionnaire-depenses-1.0.0-SNAPSHOT.war'
    Write-Host 'Arrêt de Tomcat...'
    Push-Location (Join-Path $dossierTomcat 'bin')
    try {
        & '.\shutdown.bat'
    } finally {
        Pop-Location
    }
    Start-Sleep -Seconds 5

    Write-Host 'Déploiement de la nouvelle version...'
    Copy-Item -LiteralPath $war -Destination $destinationWar -Force

    Write-Host 'Redémarrage de Tomcat...'
    Push-Location (Join-Path $dossierTomcat 'bin')
    try {
        & '.\startup.bat'
    } finally {
        Pop-Location
    }

    $ancienneValidationCertificat = [System.Net.ServicePointManager]::ServerCertificateValidationCallback
    [System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
    $disponible = $false
    for ($essai = 0; $essai -lt 30; $essai++) {
        Start-Sleep -Seconds 2
        try {
            $requeteHttps = [System.Net.HttpWebRequest]::Create($urlApplication)
            $requeteHttps.Timeout = 3000
            $reponseHttps = $requeteHttps.GetResponse()
            $statutHttps = [int]$reponseHttps.StatusCode
            $reponseHttps.Close()
            if ($statutHttps -eq 200) {
                $disponible = $true
                break
            }
        } catch {
            if ($_.Exception.Response) {
                $_.Exception.Response.Close()
            }
        }
    }
    if (-not $disponible) {
        [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $ancienneValidationCertificat
        throw "Tomcat n'a pas répondu en HTTPS à l'adresse attendue. Consultez les journaux dans $dossierTomcat\logs."
    }

    $requeteHttp = [System.Net.HttpWebRequest]::Create($urlHttpApplication)
    $requeteHttp.AllowAutoRedirect = $false
    try {
        $reponseHttp = $requeteHttp.GetResponse()
    } catch [System.Net.WebException] {
        $reponseHttp = $_.Exception.Response
        if (-not $reponseHttp) {
            [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $ancienneValidationCertificat
            throw
        }
    }
    $emplacementHttps = $reponseHttp.Headers['Location']
    $statutHttp = [int]$reponseHttp.StatusCode
    $reponseHttp.Close()
    [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $ancienneValidationCertificat
    if ($statutHttp -lt 300 -or $statutHttp -ge 400 -or -not $emplacementHttps.StartsWith('https://') -or ([uri]$emplacementHttps).Port -ne 8443) {
        throw 'La redirection HTTP vers HTTPS ne fonctionne pas correctement.'
    }

    Write-Host 'HTTPS répond correctement et HTTP redirige vers HTTPS. Ouverture du navigateur...'
    Start-Process $urlApplication
} catch {
    Write-Host "Échec du déploiement : $($_.Exception.Message)" -ForegroundColor Red
    $deploiementEchoue = $true
} finally {
    Read-Host 'Appuyez sur Entrée pour fermer cette fenêtre'
}
if ($deploiementEchoue) {
    exit 1
}
