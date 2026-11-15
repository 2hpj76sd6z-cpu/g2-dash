# Actualizaciones de G² Dash para Android

## Qué hace el actualizador

Al abrir o reanudar G² Dash, la app consulta las Releases públicas de este repositorio como máximo cada 12 horas. Si encuentra una versión estable superior, ofrece descargarla. Tras la descarga, Android abre su instalador y pide confirmar la instalación.

El APK se descarga al almacenamiento privado temporal de la app. El actualizador comprueba que el paquete sea `com.dobleg.g2dash`, que la versión sea superior y, cuando GitHub proporciona un SHA-256, valida ese hash. Android también verifica la firma del paquete antes de instalarlo.

La instalación no es silenciosa en un estéreo Android convencional. El sistema puede pedir permiso para instalar desde G² Dash y siempre controla la confirmación final.

## Requisito de firma

Todas las versiones publicadas deben usar **la misma llave privada de firma**. Si se pierde o cambia, Android no aceptará la actualización encima de la app instalada. La APK de depuración de GitHub Actions no debe considerarse una base de actualización estable.

Antes de migrar la APK que ya está instalada en el estéreo, confirma si tienes el archivo de llave con el que fue firmada. Si no existe esa llave, la primera migración a una firma estable puede requerir desinstalar la app actual; eso puede borrar sus ajustes locales.

## Preparar GitHub Actions

Genera una llave de publicación fuera del repositorio y conserva una copia de seguridad cifrada. No subas el archivo `.jks` al repositorio.

En GitHub, abre **Settings → Secrets and variables → Actions** y crea estos secretos:

- `G2DASH_KEYSTORE_BASE64`: archivo JKS convertido a Base64.
- `G2DASH_KEYSTORE_PASSWORD`: contraseña del almacén.
- `G2DASH_KEY_ALIAS`: alias de la llave.
- `G2DASH_KEY_PASSWORD`: contraseña de la llave.

No compartas esos valores en commits ni en incidencias públicas. El flujo `G² Dash · Publicar APK de Android` usa los secretos para firmar la APK.

## Publicar una versión

Después de actualizar `versionName` y el código, crea y sube una etiqueta estable, por ejemplo:

    git tag v0.5.6
    git push origin v0.5.6

GitHub Actions compila la APK de lanzamiento firmada y la adjunta a una GitHub Release. G² Dash detecta esa versión al conectarse a Internet.

El código de versión Android se deriva de la etiqueta para que cada versión posterior tenga un código mayor.
