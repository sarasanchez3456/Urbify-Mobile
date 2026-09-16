# Firma y build de release

Esta guía explica cómo compilar la variante `release` para distribución real:
firma de producción, minificación con R8 y verificación de que el APK/AAB
arranca correctamente.

## 1. Generar el keystore de producción (una sola vez)

Nunca reutilices el keystore de `debug` para producción. Generá uno nuevo y
guardalo fuera del repositorio, en un lugar seguro (gestor de contraseñas,
bóveda del equipo, secret manager del CI):

```bash
keytool -genkeypair -v \
  -keystore urbify-release.jks \
  -alias urbify-release \
  -keyalg RSA -keysize 2048 -validity 10000
```

Guardá el `.jks` generado y las contraseñas (store + key) en tu gestor de
secretos. Si se pierden, no vas a poder volver a firmar actualizaciones de la
misma app en Play Store.

## 2. Configuración local (desarrollo)

1. Copiá `keystore.properties.example` a `keystore.properties` en la raíz del
   proyecto (mismo nivel que `settings.gradle.kts`).
2. Completá los 4 valores con la ruta real del `.jks` y las contraseñas.
3. `keystore.properties` ya está en `.gitignore` — Git nunca lo va a versionar.
   No lo agregues a mano con `git add -f`.

```properties
storeFile=/ruta/absoluta/urbify-release.jks
storePassword=...
keyAlias=urbify-release
keyPassword=...
```

Con esto configurado, `./gradlew assembleRelease` o `./gradlew bundleRelease`
firman automáticamente con la clave de producción.

## 3. Configuración en CI (sin `keystore.properties`)

En CI no existe `keystore.properties`; en su lugar, `app/build.gradle.kts` lee
estas variables de entorno como alternativa:

| Variable                  | Contenido                                   |
|----------------------------|----------------------------------------------|
| `RELEASE_STORE_FILE`       | Ruta al `.jks` (subido como artefacto/secret) |
| `RELEASE_STORE_PASSWORD`   | Password del keystore                        |
| `RELEASE_KEY_ALIAS`        | Alias de la clave                            |
| `RELEASE_KEY_PASSWORD`     | Password de la clave                         |

Configurá estas 4 variables como *secrets* del pipeline (nunca como texto
plano en el YAML del workflow) y subí el `.jks` como secret file, decodificado
a un path temporal antes del build.

## 4. Qué pasa si no hay firma configurada

`assembleRelease` / `bundleRelease` fallan a propósito con un mensaje claro
("Falta la firma de release...") en vez de compilar silenciosamente con la
firma de `debug`. Esto evita distribuir por error un build firmado con debug.

## 5. Minificación (R8)

`buildTypes.release` tiene `optimization { enable = true }` (DSL nueva de
AGP), que activa shrink de código + recursos con R8. Las reglas de keep del
proyecto viven en `app/src/main/keepRules/*.keep` (todo archivo `.keep` en esa
carpeta se combina automáticamente).

Los modelos de red (`data/model/*.kt`) usan `@SerializedName` de Gson, que ya
viene protegido por las *consumer rules* que trae la librería. Si al probar el
release aparece un crash por una clase eliminada/renombrada por R8, agregá una
regla puntual en `app/src/main/keepRules/rules.keep` — evitá reglas
`-keep class ** { *; }` amplias, apuntá a la clase específica.

## 6. Verificar el build release

```bash
./gradlew assembleRelease
# o para Play Store:
./gradlew bundleRelease
```

Después, instalá el APK en un emulador/dispositivo y confirmá que la app abre
sin crashear:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.example.appcrud/.MainActivity
adb logcat | grep -i "AndroidRuntime\|FATAL"
```
