# Urbify Mobile

Aplicación Android para la plataforma **Urbify** — marketplace de servicios urbanos que conecta clientes con proveedores (plomeros, electricistas, pintores, etc.).

---

## Funcionalidades implementadas

### Módulo de autenticación
- Login y registro con JWT persistido en DataStore
- `SessionViewModel` compartido en toda la app (nombre, rol, id del usuario)
- Pantalla de perfil con edición de datos y cierre de sesión

### Navegación por rol
- **Cliente**: Home → Catálogo → Mis Solicitudes → Perfil
- **Proveedor**: Home → Mis Servicios → Solicitudes Recibidas → Perfil
- Bottom bar se adapta automáticamente según el rol

### CRUD de servicios (Proveedor)
- Listar, crear, editar y eliminar servicios propios
- Selector de categoría con dropdown

### Solicitudes
- Clientes pueden crear solicitudes desde el catálogo
- Vista de detalle con acciones según estado (aceptar, rechazar, iniciar, completar, cancelar)
- Historial separado para cliente y proveedor

### Calificaciones
- El cliente califica al proveedor al completarse un servicio
- El cliente puede **editar** o **eliminar** su propia calificación desde el historial
- Historial con promedio y estrellas

### Proveedores cercanos
- Lista de proveedores ordenada por distancia (GPS)
- **Vista de mapa** con pin en la ubicación del usuario y marcadores para cada proveedor
- Filtro de radio: 2 km, 5 km, 10 km
- Toggle Lista / Mapa en la misma pantalla

### Buscador y filtro por categoría
- Barra de búsqueda en Home navega al catálogo con el término
- Click en categoría filtra el catálogo directamente

### Tema claro / oscuro persistido
- El toggle de tema se guarda en DataStore y sobrevive al cierre de la app

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material3 |
| Navegación | Navigation Compose |
| Estado | ViewModel + StateFlow |
| Persistencia | DataStore Preferences (token JWT + preferencia de tema) |
| HTTP | Retrofit 2 + OkHttp + Gson |
| Mapas | Google Maps SDK + Maps Compose |
| Ubicación | Google Play Services Location |
| Lenguaje | Kotlin |
| Build | Gradle con Kotlin DSL |

---

## Requisitos previos

- **Android Studio** Hedgehog o superior
- **JDK 11**
- **Android SDK** con API Level 24+ instalado
- **Emulador** Android o dispositivo físico con Android 7.0+
- **Backend Urbify** corriendo localmente en el puerto `4000`
- **API Key de Google Maps** (solo necesaria para usar la vista de mapa)

---

## Configuración de Google Maps (obligatorio para la vista de mapa)

### 1. Obtener una API Key

1. Ve a [Google Cloud Console](https://console.cloud.google.com/).
2. Crea un proyecto nuevo o selecciona uno existente.
3. En el menú lateral ve a **APIs y servicios → Biblioteca**.
4. Busca **Maps SDK for Android** y actívalo.
5. Ve a **APIs y servicios → Credenciales → Crear credencial → Clave de API**.
6. Copia la clave generada.

### 2. Restringir la clave (recomendado)

En la configuración de la clave:
- **Restricción de aplicación**: Aplicaciones Android
- Agrega el paquete `com.example.appcrud` y la huella SHA-1 del certificado de debug:

```bash
# Obtener la huella SHA-1 del keystore de debug
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### 3. Agregar la clave al proyecto

Abre el archivo `app/src/main/AndroidManifest.xml` y reemplaza el valor del placeholder:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_MAPS_API_KEY_HERE" />   <!-- reemplaza este valor -->
```

Por tu clave real:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSy_TU_CLAVE_AQUI" />
```

> **Nunca subas la API key al repositorio.** Para proyectos de producción, usa `local.properties` + `buildConfigField` para mantenerla fuera del control de versiones.

---

## Cómo ejecutar

### 1. Backend

El cliente HTTP apunta a `http://10.0.2.2:4000/api/` — esta IP es la forma en que el emulador de Android accede a `localhost` del equipo host.

Levanta el servidor backend antes de lanzar la app:

```bash
# Desde el directorio del backend
npm install
npm start
# El servidor debe quedar escuchando en el puerto 4000
```

Si usas un **dispositivo físico** (no emulador), cambia la `BASE_URL` en `RetrofitClient.kt`:

```kotlin
// Reemplaza con la IP local de tu máquina en la red WiFi
private const val BASE_URL = "http://192.168.x.x:4000/api/"
```

### 2. API Key de Google Maps

Sigue los pasos de la sección anterior antes de ejecutar la app. Sin la API key el resto de la app funciona con normalidad, pero la pestaña **Mapa** en Proveedores Cercanos mostrará una pantalla en blanco.

### 3. App Android

1. Abre el proyecto en Android Studio (`File → Open → Urbify-Mobile`).
2. Espera a que Gradle sincronice las dependencias (descarga automática ~primera vez 2-5 min).
3. Agrega tu API Key de Google Maps en `AndroidManifest.xml` (ver sección anterior).
4. Selecciona un emulador o dispositivo conectado.
5. Presiona **Run ▶** o `Shift + F10`.

---

## Endpoints de la API usados

### Autenticación
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/registro` | Crea una cuenta nueva |
| `POST` | `/api/auth/login` | Inicia sesión, devuelve JWT + usuario |
| `GET` | `/api/auth/perfil` | Obtiene el perfil del usuario autenticado |
| `PUT` | `/api/auth/perfil` | Actualiza datos del perfil |

### Servicios
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/servicios/destacados` | Servicios destacados para el Home |
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
| `GET` | `/api/solicitudes/proveedor` | Solicitudes recibidas del proveedor |
| `PUT` | `/api/solicitudes/:id/estado` | Cambiar estado |

### Calificaciones
| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/calificaciones` | Crear calificación |
| `GET` | `/api/calificaciones/proveedor/:id` | Historial de un proveedor |
| `PUT` | `/api/calificaciones/:id` | Editar calificación propia |
| `DELETE` | `/api/calificaciones/:id` | Eliminar calificación propia |

### Proveedores
| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/proveedores/cercanos?lat=&lng=&radio=` | Proveedores cercanos con distancia |

Todos los endpoints protegidos reciben el token en el header:
```
Authorization: Bearer <jwt_token>
```

### Roles de usuario
| Rol | Descripción |
|---|---|
| `cliente` | Busca servicios, crea solicitudes y califica proveedores |
| `proveedor` | Gestiona sus servicios y atiende solicitudes |
| `admin` | Acceso administrativo |

---

## Estructura del proyecto

```
app/src/main/java/com/example/appcrud/
├── data/
│   ├── api/
│   │   ├── ApiService.kt               # Interfaz Retrofit con todos los endpoints
│   │   └── RetrofitClient.kt           # OkHttp con interceptores de auth y charset
│   ├── location/
│   │   └── LocationProvider.kt         # GPS con Play Services Location
│   ├── model/                          # Data classes (Usuario, Solicitud, Servicio…)
│   ├── repository/                     # Repositorios por dominio
│   └── session/
│       ├── TokenManager.kt             # DataStore + caché en memoria del JWT
│       └── ThemeManager.kt             # DataStore para la preferencia de tema
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt                 # Rutas, NavHost, SessionViewModel activity-scoped
│   │   └── BottomNavigationBar.kt      # Bottom bar adaptable por rol
│   ├── screens/
│   │   ├── AuthScreen.kt               # Login + Registro (tabs)
│   │   ├── HomeScreen.kt               # Dashboard con buscador y categorías
│   │   ├── PerfilScreen.kt             # Ver y editar perfil + cerrar sesión
│   │   ├── CatalogoScreen.kt           # Búsqueda y catálogo de servicios
│   │   ├── CreateSolicitudScreen.kt
│   │   ├── DetalleSolicitudScreen.kt   # Detalle con acciones por estado y rol
│   │   ├── MisSolicitudesClienteScreen.kt
│   │   ├── MisSolicitudesProveedorScreen.kt
│   │   ├── CalificarScreen.kt
│   │   ├── HistorialCalificacionesScreen.kt  # Con editar/eliminar calificación propia
│   │   ├── ProveedoresCercanosScreen.kt      # Lista + vista de mapa con Google Maps
│   │   ├── MisServiciosScreen.kt
│   │   ├── CreateEditServicioScreen.kt
│   │   └── StatsScreen.kt
│   ├── viewmodel/
│   │   ├── SessionViewModel.kt         # Estado de sesión compartido (usuario + rol)
│   │   ├── AuthViewModel.kt
│   │   ├── HomeViewModel.kt
│   │   ├── SolicitudViewModel.kt
│   │   ├── CalificacionViewModel.kt
│   │   ├── ProveedoresCercanosViewModel.kt
│   │   ├── MisServiciosViewModel.kt
│   │   └── CreateEditServicioViewModel.kt
│   └── theme/                          # Colores, tipografía y tema Material3
├── MainActivity.kt                     # Lee tema de DataStore al iniciar
└── UrbifyApplication.kt                # Inicializa TokenManager y ThemeManager
```

---

## Permisos requeridos

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Los permisos de ubicación son necesarios para la funcionalidad de **Proveedores Cercanos** (lista y mapa). La app los solicita en tiempo de ejecución la primera vez que el usuario accede a esa pantalla.
