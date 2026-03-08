# Ubicación de Iconos en TopBar - MiApp.kt

## Resumen
Los iconos de la TopBar se definen usando **Material Design 3 Icons** importados desde `androidx.compose.material.icons.filled.*`

Ubicación en el código: **líneas 265-370 aprox.**

---

## Desglose de Iconos por Sección

### 1. Botón de Menú Principal (NavigationIcon)
**Línea: ~268**
```kotlin
Icon(Icons.Default.Menu, contentDescription = "Menú")
```
- **Icono**: `Icons.Default.Menu`
- **Ubicación**: Esquina izquierda de la TopBar (navigationIcon)
- **Función**: Abre el menú desplegable principal

---

## 2. Menú Desplegable: "Copias de Seguridad"
**Línea: ~282**

| Item | Icono | Código | Función |
|------|-------|--------|---------|
| **Copias de Seguridad (header)** | `Icons.Default.SaveAlt` | `Icon(Icons.Default.SaveAlt, null)` | Submenu de backups |
| **Flecha derecha (expandir)** | `Icons.Default.KeyboardArrowRight` | `Icon(Icons.Default.KeyboardArrowRight, null)` | Indicador de submenu |
| **Guardar Backup** | `Icons.Default.Backup` | `Icon(Icons.Default.Backup, null)` | Exportar BD |
| **Restaurar Backup** | `Icons.Default.Restore` | `Icon(Icons.Default.Restore, null)` | Importar BD |

---

## 3. Menú Desplegable: "Tablas de Referencia"
**Línea: ~318**

| Item | Icono | Código | Función |
|------|-------|--------|---------|
| **Tablas de Referencia (header)** | `Icons.Default.TableChart` | `Icon(Icons.Default.TableChart, null)` | Submenu de tablas |
| **Flecha derecha (expandir)** | `Icons.Default.KeyboardArrowRight` | `Icon(Icons.Default.KeyboardArrowRight, null)` | Indicador de submenu |
| **Categorías Tareas** | `Icons.Default.TableChart` | `Icon(Icons.Default.TableChart, null)` | Gestionar categorías de tareas |
| **Categorías Hábitos** | `Icons.Default.TableChart` | `Icon(Icons.Default.TableChart, null)` | Gestionar categorías de hábitos |

---

## 4. Menú Principal: "Configuración"
**Línea: ~345**

| Item | Icono | Código | Función |
|------|-------|--------|---------|
| **Configuración** | `Icons.Default.Settings` | `Icon(Icons.Default.Settings, null)` | Abrir pantalla de configuración |

---

## 5. Acciones de la TopBar (Actions)
**Línea: ~356 - AccionesTopBarTareas**

En la pantalla de **Tareas**, se delega a `AccionesTopBarTareas` que contiene:
- Icono de búsqueda
- Icono de micrófono para voz
- Otro icono de acciones (revisar en `AccionesTopBarTareas.kt`)

**Ubicación del archivo**: 
```
app/src/main/java/com/example/mistareasapp/ui/components/tasks/AccionesTopBarTareas.kt
```

---

## Resumen de Todos los Iconos Utilizados

```
- Icons.Default.Menu ...................... Menú principal (hamburguesa)
- Icons.Default.SaveAlt ................... Copias de seguridad
- Icons.Default.KeyboardArrowRight ........ Expandir submenú
- Icons.Default.Backup .................... Guardar backup
- Icons.Default.Restore ................... Restaurar backup
- Icons.Default.TableChart ................ Tablas de referencia
- Icons.Default.Settings .................. Configuración
- (Otros en AccionesTopBarTareas) ......... Búsqueda, voz, etc.
```

---

## Importación de Iconos
**Línea: ~32**
```kotlin
import androidx.compose.material.icons.filled.*
```

Esto importa **todos** los iconos de Material Design 3 en modo `filled`.

---

## Notas Técnicas
1. **Material Design 3**: Se usa `androidx.compose.material.icons.filled.*`
2. **Estructura**: NavigationIcon (izquierda) + Actions (derecha)
3. **Submenús**: Se usan `DropdownMenu` con offset para posicionar correctamente
4. **Acciones dinámicas**: Las actions cambian según la ruta (solo se muestran en `PantallaTareas`)
5. **ColorDescription**: Algunos iconos tienen `null` como descripción; deberían tener descripciones accesibles

---

## Archivos Relacionados
- **MiApp.kt** → Definición de TopBar y menús principales
- **AccionesTopBarTareas.kt** → Acciones específicas de pantalla de tareas
- **Material Icons** → Biblioteca: `androidx.compose.material.icons`

