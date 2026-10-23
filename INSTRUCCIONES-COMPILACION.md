# Hunter System 38.50 — GitHub Actions y firma de APK

El código fuente Android **no está incluido en este archivo de configuración**. Para compilar, importa a la raíz del repositorio el contenido de la carpeta `Hunter-System-Native/` del ZIP de fuentes, incluyendo `app/`, `gradle/`, `gradlew` y los recursos multimedia.

## Secretos de GitHub Actions

Crear en `Settings > Secrets and variables > Actions > New repository secret`:

- `HUNTER_KEYSTORE_BASE64`: contenido **Base64** del archivo original de firma `.p12`. El contenido debe codificarse en una sola línea, sin incluir el nombre del archivo.
- `HUNTER_STORE_PASSWORD`: contraseña del almacén PKCS12 y de la clave de firma.

El build original usa `HUNTER_KEYSTORE` como variable de entorno para la **ruta local** del archivo, que el workflow reconstruye a partir de `HUNTER_KEYSTORE_BASE64`. El alias fijo en Gradle es `hunter-system-release`.

### Convertir .p12 a Base64

Linux:

```bash
base64 -w 0 hunter-system-release.p12
```

macOS:

```bash
base64 < hunter-system-release.p12 | tr -d '\n'
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\\ruta\\hunter-system-release.p12"))
```

Copia el resultado directamente al valor del secreto; no lo pongas en archivos versionados, issues ni comentarios.

## Compilar

Una vez que el código fuente esté en el repositorio y ambos secretos configurados, visita **Actions > Compilar Hunter System Android APK > Run workflow**. Al concluir correctamente, descarga el artefacto `Hunter-System-38.50-release-apk`.

No uses una clave nueva para actualizar una APK con el mismo identificador `com.huntersystem.app`, si necesitas conservar la instalación anterior. Comprueba que el certificado de tu almacén coincide con la firma de la versión que quieres actualizar.
