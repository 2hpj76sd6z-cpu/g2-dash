# Actualizaciones automáticas de G² Dash para Android

## Qué hace el actualizador

- Busca la versión estable más reciente al abrir G² Dash y, si ya pasó el intervalo, cada 12 horas mientras la app permanece activa.
- Al volver a primer plano, comprueba si venció la revisión de 12 horas.
- Descarga solo el asset `G2-Dash.apk` de una versión publicada en este repositorio.
- La instalación siempre pasa por el instalador de Android y requiere confirmación. La primera vez, Android puede pedir que permitas instalar apps desde G² Dash.

La comprobación requiere Internet. El APK no se instala de forma silenciosa.

## Firma estable requerida

Todas las versiones deben usar el mismo archivo de firma. No publiques un APK firmado con una llave distinta: Android lo rechazará como actualización.

En PowerShell, crea una llave fuera del repositorio:

```powershell
keytool -genkeypair -v -keystore g2dash-release.jks -storetype JKS -alias g2dash -keyalg RSA -keysize 2048 -validity 10000
$G2SigningBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("g2dash-release.jks"))
```

En GitHub, abre **Settings → Secrets and variables → Actions** y crea estos secretos del repositorio:

- `G2_SIGNING_KEYSTORE_BASE64`: contenido de `$G2SigningBase64`.
- `G2_SIGNING_STORE_PASSWORD`: contraseña del almacén.
- `G2_SIGNING_KEY_ALIAS`: `g2dash`.
- `G2_SIGNING_KEY_PASSWORD`: contraseña de la llave.

Conserva una copia de la llave y sus contraseñas en un respaldo privado fuera de GitHub. No las subas al repositorio ni las envíes por chat.

## Publicar una versión

Después de integrar este flujo a `main` y configurar los cuatro secretos, crea una etiqueta estable como `v0.6.0`. GitHub Actions compila y firma el APK, verifica la firma y publica `G2-Dash.apk` en la versión. El actualizador consulta las versiones estables de GitHub.

El código de versión se calcula desde la etiqueta: `0.6.0` produce un código superior al APK de desarrollo actual. Usa etiquetas semánticas `vMAJOR.MINOR.PATCH`.

## Paso único antes de activar las actualizaciones

El APK de desarrollo disponible está firmado con el certificado **Android Debug**. El flujo de depuración no conserva una llave estable de firma; por eso, una APK firmada con la nueva llave estable no se podrá instalar encima de una versión de depuración ya instalada. La migración puede requerir desinstalar la versión actual e instalar la primera estable, lo que borra los ajustes locales porque Android no tiene habilitado el respaldo de datos.

Por eso, no publiques la primera etiqueta ni reemplaces la app instalada hasta decidir cuándo hacer esa migración. Después de instalar la primera APK estable, las siguientes versiones se actualizan sobre la misma instalación usando la misma llave.
