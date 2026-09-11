# Urbify Mobile

Aplicación Android para la plataforma **Urbify** — marketplace de servicios urbanos que conecta clientes con proveedores (plomeros, electricistas, pintores, etc.).

---

## Requisitos para ejecutar el proyecto

### Software obligatorio

| Herramienta | Versión mínima | Descarga |
|---|---|---|
| Android Studio | Hedgehog (2023.1.1) o superior | [developer.android.com/studio](https://developer.android.com/studio) |
| JDK | 11 | Incluido en Android Studio |
| Android SDK | API 24 (Android 7.0) | Se instala desde Android Studio |
| Git | Cualquiera reciente | [git-scm.com](https://git-scm.com) |

### Android Studio — SDK Manager

Al abrir el proyecto por primera vez, Android Studio pedirá instalar el SDK. Asegúrate de tener:

- `Android SDK Platform 37` (o 24+)
- `Android SDK Build-Tools`
- `Google Play Services` (necesario para GPS)

Ve a `File → Settings → Appearance & Behavior → System Settings → Android SDK` y activa lo anterior.

---

## Cómo clonar y abrir el proyecto

```bash
git clone https://github.com/sarasanchez3456/Urbify-Mobile.git
cd Urbify-Mobile
```

1. Abre Android Studio
2. `File → Open` → selecciona la carpeta `Urbify-Mobile`
3. Espera a que Gradle sincronice (primera vez descarga ~200 MB de dependencias)
4. Si aparece el banner **"Gradle files have changed"** → clic en **Sync Now**

---

## Dependencias — se descargan automáticamente con Gradle

No hay que instalar nada manualmente. Gradle descarga todo al sincronizar:

| Librería | Uso |
|---|---|
| Jetpack Compose BOM | UI declarativa |
| Navigation Compose | Navegación entre pantallas |
| Material3 | Componentes visuales |
| Retrofit 2 + OkHttp | Llamadas a la API REST |
| Gson | Serialización JSON |
| DataStore Preferences | Persistencia local (JWT + tema) |
| Coil | Carga de imágenes |
| OSMDroid 6.1.18 | Mapa de proveedores cercanos (OpenStreetMap, sin API key) |
| Google Play Services Location | GPS para ubicación del usuario |
| ViewModel + StateFlow | Manejo de estado |

---

## Backend — requisito para que la app funcione

La app necesita el servidor Urbify corriendo. Sin backend no carga ninguna pantalla.

### El backend está en OTRO repositorio

> **https://github.com/sarasanchez3456/Urbify** — Backend Node.js + Frontend web + Docker

`Urbify` (web) y `Urbify-Mobile` son **repositorios Git separados** pero **dependen entre sí**: esta app consume esa misma API REST. Si trabajas aquí, clona y mantén actualizados **los dos**.

```bash
# Opción A (recomendada): todo con Docker desde el repo Urbify
git clone https://github.com/sarasanchez3456/Urbify.git
cd Urbify
docker compose up -d          # levanta db + backend (4000) + frontend

# Opción B: solo el backend a mano
cd Urbify/backend
npm install
npm start                     # debe quedar escuchando en el puerto 4000
```

### Compatibilidad de versiones

Los nombres de campo del JSON (`id`, `categoria_id`, `tarifa`, …) deben coincidir entre esta app y el backend. **Usa siempre la versión más reciente de ambos repos**: una app vieja contra un backend nuevo (o al revés) muestra campos vacíos o falla al crear servicios/solicitudes.

### Base de datos al clonar en limpio

Al levantar el backend por primera vez la base de datos queda **vacía**: el `schema.sql` solo siembra las **8 categorías**, sin usuarios ni servicios. Cada quien registra sus propios datos. Lo que tengas localmente vive en el volumen Docker `urbify_mysql_data` del backend, **nunca en el repositorio** — no se comparte al clonar.

### URL base configurada

El cliente apunta a `http://10.0.2.2:4000/api/` que es la IP del emulador para acceder a `localhost` del PC host.

**Si usas dispositivo físico** (no emulador), cambia la `BASE_URL` en `RetrofitClient.kt`:

```kotlin
// app/src/main/java/com/example/appcrud/data/api/RetrofitClient.kt
private const val BASE_URL = "http://192.168.X.X:4000/api/"
// Reemplaza con la IP local de tu PC en la red WiFi
```

Para saber tu IP local:
- **Windows**: `ipconfig` en la terminal
- **Mac/Linux**: `ifconfig | grep inet` o `ip addr`

### ⚠️ ¿Error de conexión / Timeout (10000ms) en el Emulador?

Si el backend está corriendo y funciona en el navegador del PC, pero la app te tira un error como `failed to connect to /10.0.2.2 (port 4000) ... after 10000ms`, se debe a un bloqueo del Firewall de Windows o a la configuración de red IPv6 de Node.js.

**Solución recomendada (Redirección de Puertos ADB):**
1. Abre la terminal de Android Studio o PowerShell en tu PC.
2. Redirige el puerto `4000` ejecutando el siguiente comando:
   ```powershell
   & "C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:4000 tcp:4000
   ```
3. En la app Android, ve a `RetrofitClient.kt` y cambia la URL base para que use `127.0.0.1` (localhost redirigido):
   ```kotlin
   private const val BASE_URL = "http://127.0.0.1:4000/api/"
   ```
4. Vuelve a ejecutar la app. Esto se salta las restricciones del Firewall de Windows y conecta de forma directa e instantánea.

---

## Ejecutar la app

1. Conecta un emulador o dispositivo físico con Android 7.0+
2. Asegúrate de que el backend esté corriendo
3. Presiona **Run ▶** en Android Studio o `Shift + F10`

---

## Funcionalidades implementadas

### Autenticación
- Login y registro con JWT
- Token persistido en DataStore — sobrevive al cierre de la app
- `SessionViewModel` compartido en toda la app (nombre, rol, id)
- Pantalla de perfil con edición de datos y cierre de sesión

### Navegación por rol
- **Cliente**: Home → Catálogo → Mis Solicitudes → Perfil
- **Proveedor**: Home → Mis Servicios → Solicitudes Recibidas → Perfil
- Bottom bar se adapta automáticamente según el rol registrado

### Home
- Dashboard con solicitudes activas y completadas
- Buscador que navega al catálogo con el término escrito
- Acceso rápido a categorías con filtro automático
- Proveedores destacados

### Catálogo y búsqueda
- Lista de categorías clicables
- Búsqueda por texto en tiempo real
- Filtro por categoría desde Home o desde el catálogo

### Solicitudes
- Cliente crea solicitudes desde el catálogo
- Vista de detalle con acciones según estado y rol
- Cliente puede cancelar solicitudes en estado pendiente
- Proveedor puede aceptar, rechazar, iniciar y completar
- Historial separado para cliente y proveedor

### Calificaciones
- El cliente califica al proveedor cuando se completa un servicio
- El cliente puede **editar** su calificación (estrellas + comentario)
- El cliente puede **eliminar** su calificación
- Historial con promedio y todas las opiniones

### Proveedores cercanos
- Lista de proveedores ordenada por distancia (requiere permiso GPS)
- **Vista de mapa interactivo** con OpenStreetMap (sin API key)
- Pin en la ubicación del usuario + marcadores por proveedor
- Toggle Lista / Mapa en la misma pantalla
- Filtro de radio: 2 km, 5 km, 10 km

### CRUD de servicios (Proveedor)
- Listar, crear, editar y eliminar servicios propios
- Selector de categoría con dropdown
- Confirmación antes de eliminar

### Tema claro / oscuro
- Toggle en el Home
- Preferencia guardada en DataStore — persiste entre sesiones

---

## Estructura del proyecto

```
app/src/main/java/com/example/appcrud/
│
├── data/
│   ├── api/
│   │   ├── ApiService.kt               # Todos los endpoints Retrofit
│   │   └── RetrofitClient.kt           # OkHttp + interceptores (auth, logging)
│   ├── location/
│   │   └── LocationProvider.kt         # GPS con FusedLocationProviderClient
│   ├── model/                          # Data classes de la API
│   │   ├── Auth.kt                     # LoginRequest, RegistroRequest, AuthResponse
│   │   ├── AuthError.kt                # Error de auth con bloqueo por intentos
│   │   ├── Calificacion.kt
│   │   ├── Categoria.kt
│   │   ├── ProveedorCercano.kt         # Incluye latitud/longitud para el mapa
│   │   ├── Servicio.kt
│   │   ├── Solicitud.kt                # + EstadoSolicitud + EstadoUpdateRequest
│   │   ├── Stats.kt
│   │   └── Usuario.kt                  # + object Rol
│   ├── repository/                     # Una clase por dominio, llaman a ApiService
│   └── session/
│       ├── TokenManager.kt             # JWT en DataStore + caché en memoria
│       └── ThemeManager.kt             # Preferencia de tema en DataStore
│
├── ui/
│   ├── components/
│   │   ├── EmptyState.kt               # Pantalla vacía reutilizable
│   │   └── QuickActionCard.kt          # Tarjeta de acción rápida
│   ├── navigation/
│   │   ├── NavGraph.kt                 # Todas las rutas + SessionViewModel activity-scoped
│   │   └── BottomNavigationBar.kt      # Bottom bar adaptable por rol
│   ├── screens/
│   │   ├── AuthScreen.kt               # Login + Registro (tabs)
│   │   ├── HomeScreen.kt               # Dashboard principal
│   │   ├── PerfilScreen.kt             # Ver/editar perfil + cerrar sesión
│   │   ├── CatalogoScreen.kt           # Búsqueda y categorías
│   │   ├── CreateSolicitudScreen.kt    # Nueva solicitud
│   │   ├── DetalleSolicitudScreen.kt   # Detalle con acciones por estado/rol
│   │   ├── MisSolicitudesClienteScreen.kt
│   │   ├── MisSolicitudesProveedorScreen.kt
│   │   ├── CalificarScreen.kt          # Enviar calificación con estrellas
│   │   ├── HistorialCalificacionesScreen.kt  # Ver/editar/eliminar calificaciones
│   │   ├── ProveedoresCercanosScreen.kt      # Lista + mapa OSMDroid
│   │   ├── MisServiciosScreen.kt       # CRUD de servicios del proveedor
│   │   ├── CreateEditServicioScreen.kt # Crear o editar servicio
│   │   └── StatsScreen.kt             # Estadísticas globales
│   ├── viewmodel/                      # Un ViewModel por pantalla
│   └── theme/
│       ├── Color.kt                    # Paleta de marca Urbify (light + dark)
│       ├── Theme.kt                    # UrbifyTheme + UrbifyPrimaryGradient
│       └── Type.kt                     # Tipografía
│
├── MainActivity.kt                     # Lee tema de DataStore al iniciar
└── UrbifyApplication.kt               # Inicializa TokenManager, ThemeManager y OSMDroid
```

---

## Endpoints de la API

### Autenticación
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/registro` | Crear cuenta |
| `POST` | `/api/auth/login` | Iniciar sesión → devuelve JWT + usuario |
| `GET` | `/api/auth/perfil` | Perfil del usuario autenticado |
| `PUT` | `/api/auth/perfil` | Actualizar datos del perfil |

### Servicios
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/servicios/destacados` | Servicios para el Home |
| `GET` | `/api/servicios/buscar?q=` | Búsqueda por texto |
| `GET` | `/api/servicios/categoria/:id` | Filtrar por categoría |
| `GET` | `/api/servicios/mios` | Servicios del proveedor autenticado |
| `POST` | `/api/servicios` | Crear servicio |
| `PUT` | `/api/servicios/:id` | Editar servicio |
| `DELETE` | `/api/servicios/:id` | Eliminar servicio |

### Solicitudes
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/solicitudes` | Crear solicitud |
| `GET` | `/api/solicitudes/cliente` | Solicitudes del cliente |
| `GET` | `/api/solicitudes/proveedor` | Solicitudes recibidas |
| `PUT` | `/api/solicitudes/:id/estado` | Cambiar estado |

### Calificaciones
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/calificaciones` | Crear calificación |
| `GET` | `/api/calificaciones/proveedor/:id` | Historial de un proveedor |
| `PUT` | `/api/calificaciones/:id` | Editar calificación propia |
| `DELETE` | `/api/calificaciones/:id` | Eliminar calificación propia |

### Otros
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/categorias` | Lista de categorías |
| `GET` | `/api/proveedores/cercanos?lat=&lng=&radio=` | Proveedores con distancia |
| `GET` | `/api/stats` | Estadísticas globales |

Todos los endpoints protegidos requieren:
```
Authorization: Bearer <jwt_token>
```

### Roles
| Rol | Acceso |
|---|---|
| `cliente` | Busca servicios, crea solicitudes, califica proveedores |
| `proveedor` | Gestiona servicios, atiende solicitudes |
| `admin` | Acceso administrativo |

---

## ¿Dónde se guardan los datos?

### En el servidor (base de datos del backend)

Todos los datos de la plataforma se almacenan en la base de datos del backend, **no en el dispositivo**:

| Datos | Dónde viven |
|---|---|
| Usuarios (clientes y proveedores) | Base de datos del backend |
| Servicios publicados | Base de datos del backend |
| Solicitudes | Base de datos del backend |
| Calificaciones | Base de datos del backend |
| Categorías | Base de datos del backend |

La app **nunca guarda** esta información localmente. Cada vez que abres una pantalla, los datos se piden al servidor via la API REST (`http://10.0.2.2:4000/api/`). Si el backend no está corriendo, la app no muestra nada.

### En el dispositivo (DataStore)

La app solo guarda dos preferencias locales usando **Jetpack DataStore**:

| Dato | Archivo DataStore | Clave |
|---|---|---|
| Token JWT (sesión) | `urbify_session` | `jwt_token` |
| Tema claro/oscuro | `urbify_prefs` | `dark_theme` |

El token JWT permite que la app recuerde tu sesión aunque la cierres. Al hacer "Cerrar sesión" el token se borra del DataStore.

### ¿Qué base de datos usa el backend?

Depende de cómo está configurado el backend. Para saberlo:

1. Abre el directorio del backend y busca el archivo `.env`:
   ```
   DB_HOST=...
   DATABASE_URL=...
   ```
2. O revisa el archivo de configuración de base de datos:
   - `database.js`
   - `db.js`
   - `config/database.js`

Los backends Node.js más comunes usan **MongoDB**, **PostgreSQL** o **MySQL**.

---

## Permisos del dispositivo

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Los permisos de ubicación se solicitan en tiempo de ejecución la primera vez que el usuario accede a **Proveedores Cercanos**. Internet es necesario para toda la app (API + tiles del mapa).

---

## Cambios recientes

### v1.4 — Alineación del contrato con la API + limpieza
- **Fix**: los `@SerializedName` de los modelos no coincidían con el JSON del backend, así que servicios, solicitudes y calificaciones llegaban con campos `null` (y provocaban NPE). Renombrados a `id`, `categoria_id`, `tarifa`, `descripcion`, `servicio_titulo`, etc.
- **Fix**: los `@Body` de crear/editar enviaban claves que el backend ignoraba → ahora se crean servicios y solicitudes correctamente
- **Fix**: los endpoints de escritura devuelven `{ mensaje }`, no el recurso plano — se tipan con `MensajeResponse` en vez de deserializar un objeto vacío
- **Fix**: `getCalificacionesProveedor` esperaba una lista pero la API responde `{ calificaciones, promedio, total }`
- **Removido**: el `charsetInterceptor` de `RetrofitClient` que re-decodificaba el body como ISO-8859-1 (corrompía emojis/`€`/`–` y cualquier texto con "Ã"); el backend ya sirve UTF-8 correcto
- Requiere el backend en su versión más reciente (repo `Urbify`)

### v1.3 — Mapa OSMDroid, editar calificaciones, tema persistido + fixes
- Mapa de proveedores con **OpenStreetMap** vía OSMDroid — sin API key, sin costo
- Toggle Lista/Mapa en la pantalla de proveedores cercanos
- El cliente puede **editar** y **eliminar** sus propias calificaciones
- El tema claro/oscuro ahora **persiste** entre sesiones con DataStore
- **Fix**: `CatalogoViewModel` llamaba al endpoint de solicitudes por error al inicializar
- **Fix**: Navegar al Catálogo desde la bottom bar podía crashear al intentar parsear `{categoriaId}` como entero
- **Fix**: El ícono "Guardar" del AppBar en Perfil cerraba el modo edición sin guardar

### v1.2 — Buscador, filtro por categoría, detalle de solicitud
- Barra de búsqueda en Home navega al catálogo con el término
- Clic en categoría filtra el catálogo automáticamente
- Pantalla de detalle de solicitud con acciones por estado y rol
- Cliente puede cancelar solicitudes pendientes con confirmación

### v1.1 — Navegación por rol y CRUD de servicios
- Bottom bar adaptable: cliente ve Catálogo/Solicitudes, proveedor ve Servicios/Pedidos
- Pantallas completas de gestión de servicios (crear, editar, eliminar)
- Selector de categoría con dropdown en el formulario de servicio

### v1.0 — Autenticación completa
- Login y registro con JWT persistido en DataStore
- `SessionViewModel` compartido en toda la app
- Pantalla de perfil con edición y cierre de sesión
