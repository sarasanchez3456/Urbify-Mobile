# Urbify Mobile

Aplicación Android para la plataforma **Urbify** — marketplace de servicios urbanos que conecta clientes con proveedores (plomeros, electricistas, pintores, etc.).

---

## Qué se implementó en este commit

### Módulo de autenticación completo

#### Problema previo
El flujo de login/registro ya existía parcialmente: guardaba el token JWT en DataStore, pero descartaba el objeto `Usuario` que devuelve la API. Esto obligaba a cada pantalla a hacer una llamada extra a `GET /api/auth/perfil` para saber quién era el usuario logueado.

#### Solución implementada

**`SessionViewModel`** — estado compartido, scoped a la Activity:
- Almacena el `Usuario` logueado (nombre, apellido, correo, rol, id) en memoria durante toda la sesión.
- Expone `rol` e `idUsuario` para que cualquier pantalla sepa con qué tipo de cuenta trabaja.
- Provee `cargarPerfil()` (`GET /api/auth/perfil`), `actualizarPerfil()` (`PUT /api/auth/perfil`) y `logout()` (borra DataStore + estado en memoria).

**`AuthViewModel`** — actualizado:
- `login()` y `registro()` ahora retornan el `AuthResponse` completo (token + usuario).
- Expone `usuarioLogueado: Usuario?` en el estado para que el NavGraph lo inyecte en `SessionViewModel` al navegar.

**`PerfilScreen`** — pantalla real (reemplaza el placeholder "próximamente"):
- **Modo vista**: avatar con inicial, nombre completo, badge de rol, campos de información.
- **Modo edición**: formulario para actualizar nombre, apellido, teléfono, dirección y oficio (oficio solo visible para proveedores). El icono del AppBar alterna entre ✏️ y 💾.
- Botón **Cerrar sesión** que limpia token + estado y regresa a la pantalla de login.
- Snackbar de confirmación al guardar o error si falla.

**`HomeScreen`** — ya no hace una llamada extra al perfil:
- Lee `usuario` directamente de `SessionViewModel`.
- El saludo se adapta al género del rol: "Bienvenida de vuelta" (cliente) / "Bienvenido de vuelta" (proveedor).

**Flujo de logout** — nuevo:
- Borra el token de DataStore.
- Limpia el back stack completo y navega a LOGIN.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| UI | Jetpack Compose + Material3 |
| Navegación | Navigation Compose |
| Estado | ViewModel + StateFlow |
| Persistencia | DataStore Preferences (token JWT) |
| HTTP | Retrofit 2 + OkHttp + Gson |
| Imágenes | Coil |
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

### 2. App Android

1. Abre el proyecto en Android Studio (`File → Open → Urbify-Mobile`).
2. Espera a que Gradle sincronice las dependencias.
3. Selecciona un emulador o dispositivo conectado.
4. Presiona **Run ▶** o `Shift + F10`.

---

## Endpoints de la API usados

### Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/registro` | Crea una cuenta nueva |
| `POST` | `/api/auth/login` | Inicia sesión, devuelve JWT + usuario |
| `GET` | `/api/auth/perfil` | Obtiene el perfil del usuario autenticado |
| `PUT` | `/api/auth/perfil` | Actualiza datos del perfil |

Todos los endpoints protegidos reciben el token en el header:
```
Authorization: Bearer <jwt_token>
```

### Roles de usuario

| Rol | Descripción |
|---|---|
| `cliente` | Puede buscar servicios y crear solicitudes |
| `proveedor` | Puede recibir solicitudes y ver su historial de calificaciones |
| `admin` | Acceso administrativo |

---

## Estructura del proyecto

```
app/src/main/java/com/example/appcrud/
├── data/
│   ├── api/
│   │   ├── ApiService.kt          # Interfaz Retrofit con todos los endpoints
│   │   └── RetrofitClient.kt      # OkHttp con interceptores de auth y charset
│   ├── model/                     # Data classes (Usuario, Solicitud, Servicio…)
│   ├── repository/                # Repositorios por dominio
│   └── session/
│       └── TokenManager.kt        # DataStore + caché en memoria del JWT
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt            # Rutas, NavHost, SessionViewModel activity-scoped
│   │   └── BottomNavigationBar.kt
│   ├── screens/
│   │   ├── AuthScreen.kt          # Login + Registro (tabs)
│   │   ├── HomeScreen.kt          # Dashboard principal
│   │   ├── PerfilScreen.kt        # Ver y editar perfil + cerrar sesión
│   │   ├── CatalogoScreen.kt      # Búsqueda y catálogo de servicios
│   │   ├── CreateSolicitudScreen.kt
│   │   ├── MisSolicitudesClienteScreen.kt
│   │   ├── MisSolicitudesProveedorScreen.kt
│   │   ├── CalificarScreen.kt
│   │   ├── HistorialCalificacionesScreen.kt
│   │   ├── ProveedoresCercanosScreen.kt
│   │   └── StatsScreen.kt
│   ├── viewmodel/
│   │   ├── SessionViewModel.kt    # Estado de sesión compartido (usuario + rol)
│   │   ├── AuthViewModel.kt       # Lógica de login/registro
│   │   ├── HomeViewModel.kt       # Datos del dashboard
│   │   └── …
│   └── theme/                     # Colores, tipografía y tema Material3
├── MainActivity.kt
└── UrbifyApplication.kt           # Inicializa TokenManager al arrancar
```

---

## Permisos requeridos

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Los permisos de ubicación son necesarios para la funcionalidad de **Proveedores Cercanos**.
