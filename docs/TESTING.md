# Tests

## Cómo correrlos

```bash
# Unitarios (JVM, sin emulador, sin backend) — corren en CI en cada PR
./gradlew :app:testDevelopmentDebugUnitTest

# Instrumentados (Compose UI) — necesitan un emulador/dispositivo conectado
./gradlew :app:connectedDevelopmentDebugAndroidTest
```

Los reportes quedan en `app/build/reports/tests/testDevelopmentDebugUnitTest/index.html` y
`app/build/reports/androidTests/connected/developmentDebug/index.html`.

## Qué hay y por qué está separado así

### `app/src/test/` — unitarios, JVM puro

- **`data/api/GsonSerializationTest.kt`**: fija el contrato de
  `@SerializedName` de los modelos contra JSON con forma real del backend
  (snake_case, `disponible` como TINYINT 0/1, sobres tipo
  `CalificacionesProveedorResponse`). Usa el mismo `GsonBuilder().setLenient()`
  que `RetrofitClient`.
- **`data/repository/*RepositoryTest.kt`** (Auth, Servicio, Solicitud,
  Calificacion): arrancan un `MockWebServer` real y construyen un `ApiService`
  real (Retrofit + el mismo Gson) contra él — ver
  `testutil/TestApiServiceFactory.kt`. Cubren el happy path, el path/método/body
  exacto de la request, y errores HTTP (401/403/404/409/429/500) verificando
  que se propagan como `HttpException` con el código correcto y que
  `AuthRepository.parseError` los interpreta bien (incluido el caso "bloqueado
  tras varios intentos").
- **`ui/viewmodel/*ViewModelTest.kt`** (Auth, Session, Solicitud, Calificacion,
  ProveedoresCercanos): mockean el Repository (o `ApiService`, en el caso de
  `SessionViewModel`) con MockK y verifican transiciones de estado —
  loading → success/error, actualizaciones optimistas con revert (
  `SolicitudViewModel.cambiarEstado`, `SessionViewModel.setDisponible`), y que
  un fallo en el refresh posterior a un cambio exitoso NO revierte el cambio
  ya confirmado.

**TokenManager y `Application`**: `AuthViewModel`/`SessionViewModel` llaman a
`TokenManager.saveToken/clearToken(getApplication(), ...)`. En vez de mockear
`Application` (no se puede sustituir `TokenManager`, es un `object`), estas
pruebas usan `TokenManager.init(dataStore, scope)` con un `DataStore` de
prueba en un archivo temporal (`PreferenceDataStoreFactory.create`, sin
Robolectric) — la misma técnica que `TokenManagerTest`. La `Application`
pasada al ViewModel nunca llega a usarse porque `TokenManager` ya tiene el
`DataStore` cacheado. Como ese guardado hace I/O real en disco (fuera del
scheduler virtual de `runTest`), esos tests puntuales esperan con un poll
acotado (`awaitState`) en vez de asumir que el estado ya se resolvió apenas
vuelve la llamada — ver el comentario en `AuthViewModelTest`.

### `app/src/androidTest/` — instrumentados, Compose UI

- **`ui/navigation/UrbifyBottomBarTest.kt`**: navegación por rol — cliente ve
  Catálogo/Solicitudes, proveedor ve Trabajos/Billetera, nunca los del otro
  rol.
- **`ui/screens/AuthScreenTest.kt`**: formulario principal (login/registro) —
  validación de formato de correo, botón de submit deshabilitado hasta que
  los campos son válidos (incluido el caso proveedor-sin-oficio).
- **`ui/screens/ProveedoresCercanosScreenTest.kt`**: estados según el permiso
  de ubicación. El permiso se revoca explícitamente en `@Before`
  (`UiAutomation.revokeRuntimePermission`) para que el punto de partida sea
  determinista sin importar qué corrió antes en el mismo dispositivo/CI:
  - sin permiso y sin dirección de perfil → pantalla "Necesitamos saber dónde
    estás" con botón para pedir el permiso o elegir dirección.
  - sin permiso pero CON una dirección guardada en el perfil → cae a esa
    dirección y carga los proveedores igual, sin bloquear la pantalla en el
    pedido de permiso.

  No se testea el camino de GPS real (con permiso concedido): depende de que
  el emulador tenga una fix de ubicación, lo que lo haría un test flaky por
  motivos ajenos al código de la app.

## Qué falta (fuera de alcance de esta ronda)

- ViewModels de servicios (`MisServiciosViewModel`,
  `CreateEditServicioViewModel`), `StatsViewModel`, `CatalogoViewModel` y
  `HomeViewModel` no tienen tests todavía — el patrón para agregarlos es el
  mismo que `SolicitudViewModelTest`/`CalificacionViewModelTest` (mockear el
  repository correspondiente con MockK).
- No hay smoke test end-to-end contra un backend real (login → navegar →
  crear solicitud → calificar) en el APK compilado. Los repository tests
  cubren la integración HTTP/Gson con un servidor simulado (MockWebServer),
  pero no reemplazan una corrida manual/CI contra
  [el backend de Urbify](https://github.com/sarasanchez3456/Urbify) real
  antes de un release.
