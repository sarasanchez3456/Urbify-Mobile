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
| `RELEASE_STORE_FILE`       | Ruta al `.jks` en el runner (nunca el repo)   |
| `RELEASE_STORE_PASSWORD`   | Password del keystore                        |
| `RELEASE_KEY_ALIAS`        | Alias de la clave                            |
| `RELEASE_KEY_PASSWORD`     | Password de la clave                         |

Ya está implementado en `.github/workflows/android-release.yml` (corre solo en
tags `v*` o `workflow_dispatch` — **nunca** en `pull_request`, para que la
firma de producción no quede expuesta a un PR). Los secrets del repo que hay
que configurar en GitHub (Settings → Secrets and variables → Actions) son:

- `RELEASE_KEYSTORE_BASE64`: el `.jks` codificado en base64 (`base64 -i urbify-release.jks | pbcopy`
  en Mac, pegar el resultado como secret).
- `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`: igual
  que en local.

Cómo el workflow materializa el `.jks` de forma segura, sin subirlo como
artefacto persistente ni imprimirlo en logs:

1. Decodifica `RELEASE_KEYSTORE_BASE64` a `$RUNNER_TEMP/release.jks` — un
   directorio temporal *del runner*, no del workspace del repo, que GitHub
   destruye junto con la VM al terminar el job.
2. Nunca hace `cat`/`echo` del contenido del keystore ni de las contraseñas;
   solo exporta la *ruta* del archivo (`RELEASE_STORE_FILE`), que no es
   secreta. GitHub Actions además enmascara automáticamente en los logs
   cualquier valor que coincida con un secret configurado.
3. El `path:` del artifact que se sube al final del job (`actions/upload-artifact`)
   apunta solo a `app/build/outputs/...` (el APK/AAB) — el `.jks` en
   `$RUNNER_TEMP` queda fuera de ese glob a propósito, y además se borra
   explícitamente en un paso `if: always()` al final del job.

Si tu proveedor de CI no es GitHub Actions, el mismo patrón aplica: secret en
base64 → decodificar a un path *temporal y efímero* del runner (nunca al
workspace del repo ni a un volumen persistente) → nunca loguear su contenido →
borrarlo explícitamente al terminar.

## 4. Qué pasa si no hay firma configurada, o está incompleta/rota

`assembleRelease` / `bundleRelease` dependen de la tarea `checkReleaseSigningConfig`
(ver `app/build.gradle.kts`), que corre antes y falla con un mensaje claro en
estos casos — nunca compilan en silencio con la firma de `debug`:

- Falta alguna de las 4 propiedades: el error nombra **cuáles** (`storeFile`,
  `storePassword`, `keyAlias`, `keyPassword`) sin mostrar ningún valor.
- `storeFile` apunta a un archivo que no existe.
- `storeFile` apunta a un archivo que existe pero no se puede leer (permisos).

Podés correr solo esa verificación, sin compilar nada, con:

```bash
./gradlew checkReleaseSigningConfig
```

La tarea está implementada como una `tasks.register` normal (no
`gradle.taskGraph.whenReady`), a propósito: resuelve y captura los valores
durante la configuración en variables locales simples, y solo los valida
dentro de `doLast` — el patrón compatible con la configuration cache que
recomienda Gradle. `taskGraph.whenReady` captura una referencia viva al grafo
de tareas dentro del closure, lo cual es frágil con la configuration cache
(building con `--configuration-cache` puede fallar en reusar la cache).

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

## 6. Verificar el build release localmente

```bash
./gradlew assembleRelease
# o para Play Store:
./gradlew bundleRelease
```

Compilar no alcanza para confirmar que R8 no rompió nada: hay que instalar el
APK y usar la app de verdad.

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.example.appcrud/.MainActivity
adb logcat | grep -i "AndroidRuntime\|FATAL"
```

Con el [backend de Urbify](https://github.com/sarasanchez3456/Urbify) corriendo
(ver el README principal de este repo), ejercitá al menos:

- **Login real** (Retrofit + Gson + el interceptor de `TokenManager`): si R8
  eliminó u ofuscó algún campo de `Usuario`/`AuthError` sin la keep rule
  correspondiente, el login falla o deserializa mal en vez de solo en debug.
- **Navegación** entre Home, Catálogo y Perfil (Compose Navigation con rutas
  parametrizadas — otro punto típico donde R8 puede romper algo que en debug
  no se nota).
- **Un flujo con Compose "pesado"**: el mapa de proveedores cercanos
  (OSMDroid) o el historial de calificaciones.

## 7. Checks automáticos del PR

`.github/workflows/android-ci.yml` corre en cada PR (no necesita secrets, así
que corre igual desde forks):

- `./gradlew testDebugUnitTest` (incluye `TokenManagerTest`, etc.)
- `./gradlew lintDebug`
- `./gradlew assembleDebug`
- Confirma que `checkReleaseSigningConfig` **falla** sin firma configurada
  (es el estado normal de un PR sin secrets de release) — una regresión que
  hiciera pasar el build release sin firma real rompería este check.

`.github/workflows/android-release.yml` compila y firma la variante release de
verdad, pero solo en tags `v*` o disparado a mano (`workflow_dispatch`) — ver
la sección 3 para el manejo seguro del keystore ahí.
