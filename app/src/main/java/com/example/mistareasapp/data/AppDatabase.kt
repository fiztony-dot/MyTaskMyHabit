package com.example.mistareasapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.CategoriaDao
import com.example.mistareasapp.data.Converters
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.data.tasks.TareaDao
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.HabitoDao
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.HabitoPausa
import com.example.mistareasapp.data.habits.HabitoVersion
import com.example.mistareasapp.data.habits.TareaHabito
import com.example.mistareasapp.data.habits.TareaHabitoHistorial
import com.example.mistareasapp.data.habits.toVersion
import com.example.mistareasapp.data.shopping.ListaCategoriaProducto
import com.example.mistareasapp.data.shopping.ListaCompraDao
import com.example.mistareasapp.data.shopping.ListaItem
import com.example.mistareasapp.data.shopping.ListaLugar
import com.example.mistareasapp.data.shopping.ListaProducto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Migración de versión 5 → 6: Crear tabla de categorías
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Crear tabla de categorías si no existe
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `categorias` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `titulo` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

// Migración de versión 6 → 7: Preparación para hábitos
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Esta es una migración "vacía" para alinear versiones
        // Las tablas de hábitos se crearán en la siguiente migración
        android.util.Log.d("MIGRATION_6_7", "Migración 6→7 completada")
    }
}

// Migración de versión 7 → 8: Crear todas las tablas de hábitos
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Verificar si la tabla habitos existe antes de alterar
        try {
            database.execSQL("ALTER TABLE habitos ADD COLUMN objetivoValor INTEGER DEFAULT NULL")
        } catch (e: Exception) {
            // Si falla es porque ya existe la columna o la tabla no existe
            android.util.Log.d("MIGRATION_6_7", "Columna objetivoValor ya existe o tabla no existe")
        }

        try {
            database.execSQL("ALTER TABLE habitos ADD COLUMN unidad TEXT DEFAULT NULL")
        } catch (e: Exception) {
            android.util.Log.d("MIGRATION_6_7", "Columna unidad ya existe")
        }

        // 2. Crear tabla de categorías de hábitos de forma segura
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_categorias` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `icono` TEXT NOT NULL, 
                `color` TEXT NOT NULL
            )
        """.trimIndent())

        // 3. Crear tabla principal de hábitos de forma segura
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `descripcion` TEXT, 
                `categoriaId` INTEGER NOT NULL, 
                `fechaInicio` INTEGER NOT NULL, 
                `frecuencia` TEXT NOT NULL, 
                `vecesPorDia` INTEGER NOT NULL, 
                `objetivoValor` INTEGER,
                `unidad` TEXT,
                `objetivoRachaSemanas` INTEGER NOT NULL, 
                `recordatoriosActivos` INTEGER NOT NULL, 
                `horaRecordatorio` TEXT,
                `icono` TEXT NOT NULL, 
                `colorHex` TEXT NOT NULL, 
                `activo` INTEGER NOT NULL
            )
        """.trimIndent())

        // 4. Crear tabla de historial
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_historial` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `habitoId` INTEGER NOT NULL, 
                `fecha` INTEGER NOT NULL, 
                `valorProgreso` INTEGER NOT NULL, 
                `completado` INTEGER NOT NULL, 
                FOREIGN KEY(`habitoId`) REFERENCES `habitos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

        // 5. Crear tabla de tareas específicas de hábitos
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_tareas_especificas` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `habitoId` INTEGER NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `descripcion` TEXT, 
                `completada` INTEGER NOT NULL, 
                FOREIGN KEY(`habitoId`) REFERENCES `habitos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insertar hábitos de ejemplo SOLO si la tabla está vacía
        val fechaHoy = java.time.LocalDate.now().toEpochDay()

        // Verificar si ya hay hábitos
        val cursor = database.query("SELECT COUNT(*) FROM habitos")
        cursor.moveToFirst()
        val habitosExistentes = cursor.getInt(0)
        cursor.close()

        if (habitosExistentes == 0) {
            // Solo insertamos si no hay hábitos (primera vez)

            // 1. Insertar categorías de hábitos
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Salud', 'medical_services', '#EF5350')")
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Deporte', 'fitness_center', '#66BB6A')")
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Aprendizaje', 'school', '#42A5F5')")
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Bienestar', 'self_improvement', '#AB47BC')")

            // 2. Insertar hábitos de ejemplo
            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Inglés', 'Practicar inglés', 3, $fechaHoy, 'SEMANAL', 1, 100, 'Minutos', 4, 0, NULL, 'language', '#42A5F5', 1)
            """)

            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Bisoprolol', 'Tomar medicamento', 1, $fechaHoy, 'DIARIA', 1, 1, 'vez', 4, 0, NULL, 'medication', '#EF5350', 1)
            """)

            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Pasos', 'Caminar diariamente', 2, $fechaHoy, 'SEMANAL', 1, 40000, 'Pasos', 4, 0, NULL, 'directions_run', '#66BB6A', 1)
            """)

            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Día sin Alcohol', 'Evitar consumo', 4, $fechaHoy, 'MENSUAL', 1, 18, 'días', 4, 0, NULL, 'no_drinks', '#AB47BC', 1)
            """)
        }
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN tipoObjetivo TEXT NOT NULL DEFAULT 'FRECUENCIA'")
        database.execSQL("ALTER TABLE habitos ADD COLUMN esCompuestoPorTareas INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habitos ADD COLUMN criterioCumplimientoTareas TEXT NOT NULL DEFAULT 'TODAS'")
        database.execSQL("ALTER TABLE habitos ADD COLUMN minimoTareasCumplimiento INTEGER DEFAULT NULL")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN fechaModificacion INTEGER DEFAULT NULL")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN pausado INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habitos ADD COLUMN fechaInicioPausa INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE habitos ADD COLUMN fechaFinPausa INTEGER DEFAULT NULL")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN objetivoPorcentajeDias INTEGER DEFAULT NULL")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Días de la semana en que aplica el hábito (solo DIARIA)
        database.execSQL("ALTER TABLE habitos ADD COLUMN diasSemana TEXT DEFAULT NULL")
        // Historial de estado de tareas por fecha
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `tareas_habito_historial` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tareaId` INTEGER NOT NULL,
                `habitoId` INTEGER NOT NULL,
                `fecha` INTEGER NOT NULL,
                `completada` INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_tareas_habito_historial_tareaId_fecha` ON `tareas_habito_historial` (`tareaId`, `fecha`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_tareas_habito_historial_habitoId_fecha` ON `tareas_habito_historial` (`habitoId`, `fecha`)")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN puedeSuperar100 INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nueva tabla de versiones de definición de hábito
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_versiones` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitoId` INTEGER NOT NULL,
                `fechaInicio` INTEGER NOT NULL,
                `frecuencia` TEXT NOT NULL,
                `tipoObjetivo` TEXT NOT NULL,
                `vecesPorDia` INTEGER NOT NULL,
                `objetivoValor` INTEGER,
                `unidad` TEXT,
                `esCompuestoPorTareas` INTEGER NOT NULL,
                `minimoTareasCumplimiento` INTEGER,
                `objetivoPorcentajeDias` INTEGER,
                `puedeSuperar100` INTEGER NOT NULL DEFAULT 0,
                `diasSemana` TEXT
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habitos_versiones_habitoId_fechaInicio` " +
            "ON `habitos_versiones` (`habitoId`, `fechaInicio`)"
        )
        // Poblar con la definición actual de cada hábito (primera versión = desde fechaInicio del hábito)
        database.execSQL("""
            INSERT INTO `habitos_versiones`
                (`habitoId`, `fechaInicio`, `frecuencia`, `tipoObjetivo`, `vecesPorDia`,
                 `objetivoValor`, `unidad`, `esCompuestoPorTareas`, `minimoTareasCumplimiento`,
                 `objetivoPorcentajeDias`, `puedeSuperar100`, `diasSemana`)
            SELECT `id`, `fechaInicio`, `frecuencia`, `tipoObjetivo`, `vecesPorDia`,
                   `objetivoValor`, `unidad`, `esCompuestoPorTareas`, `minimoTareasCumplimiento`,
                   `objetivoPorcentajeDias`, `puedeSuperar100`, `diasSemana`
            FROM `habitos`
        """.trimIndent())
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nuevo campo tipoMedicion en habitos
        database.execSQL("ALTER TABLE habitos ADD COLUMN tipoMedicion TEXT NOT NULL DEFAULT 'PROPORCIONAL_CON_TOPE'")
        // Migrar: puedeSuperar100=true → PROPORCIONAL_SIN_TOPE
        database.execSQL("UPDATE habitos SET tipoMedicion = 'PROPORCIONAL_SIN_TOPE' WHERE puedeSuperar100 = 1")
        // Nuevo campo tipoMedicion en versiones
        database.execSQL("ALTER TABLE habitos_versiones ADD COLUMN tipoMedicion TEXT NOT NULL DEFAULT 'PROPORCIONAL_CON_TOPE'")
        database.execSQL("UPDATE habitos_versiones SET tipoMedicion = 'PROPORCIONAL_SIN_TOPE' WHERE puedeSuperar100 = 1")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nueva tabla para historial completo de periodos pausados
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_pausas` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitoId` INTEGER NOT NULL,
                `fechaInicio` INTEGER NOT NULL,
                `fechaFin` INTEGER
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habitos_pausas_habitoId` ON `habitos_pausas` (`habitoId`)"
        )
        // Migrar pausas existentes (campo único) a la nueva tabla
        database.execSQL("""
            INSERT INTO `habitos_pausas` (`habitoId`, `fechaInicio`, `fechaFin`)
            SELECT `id`, `fechaInicioPausa`, `fechaFinPausa`
            FROM `habitos`
            WHERE `fechaInicioPausa` IS NOT NULL
        """.trimIndent())
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN dificultad INTEGER NOT NULL DEFAULT 3")
        // Pre-cargar dificultades según tabla de referencia
        listOf(
            "Día sin Alcohol" to 3, "Push Up" to 2, "Bisoprolol" to 1,
            "Complementos Nutricionales" to 1, "Imagen" to 2, "Inglés" to 4,
            "Estiramientos" to 4, "Ejercicio" to 5, "Pasos" to 5,
            "Care Nutrition" to 4, "Tai-Chi" to 3, "Natación" to 4
        ).forEach { (nombre, dif) ->
            database.execSQL(
                "UPDATE habitos SET dificultad = $dif WHERE nombre = '${nombre.replace("'", "''")}'"
            )
        }
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Actualizar iconos de categorías por nombre (case-insensitive)
        // Ejercicio → 🤸, Salud → 🩺
        // También cubre las creadas por el importer (nombre en minúsculas)
        listOf("Ejercicio", "ejercicio", "Deporte", "deporte").forEach { nombre ->
            database.execSQL("UPDATE habitos_categorias SET icono = '🤸' WHERE LOWER(nombre) = LOWER('$nombre')")
        }
        listOf("Salud", "salud").forEach { nombre ->
            database.execSQL("UPDATE habitos_categorias SET icono = '🩺' WHERE LOWER(nombre) = LOWER('$nombre')")
        }
        // Bienestar ya tiene 🧘 en iconoAEmoji (self_improvement → 🧘), no cambia
        // Aprendizaje sin cambios
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Tabla de lugares
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `lista_lugares` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nombre` TEXT NOT NULL,
                `esDefault` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // Tabla de categorías de producto
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `lista_categorias_producto` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nombre` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL,
                `orden` INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // Tabla de productos
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `lista_productos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `nombre` TEXT NOT NULL,
                `categoriaId` INTEGER,
                `aliases` TEXT NOT NULL DEFAULT '[]',
                FOREIGN KEY(`categoriaId`) REFERENCES `lista_categorias_producto`(`id`) ON DELETE SET NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_lista_productos_categoriaId` ON `lista_productos` (`categoriaId`)")

        // Tabla de items de la lista
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `lista_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `productoId` INTEGER NOT NULL,
                `lugarId` INTEGER NOT NULL,
                `cantidad` TEXT NOT NULL DEFAULT '1',
                `unidad` TEXT NOT NULL DEFAULT '',
                `marcadoComprado` INTEGER NOT NULL DEFAULT 0,
                `fechaCreacion` INTEGER NOT NULL,
                FOREIGN KEY(`productoId`) REFERENCES `lista_productos`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`lugarId`) REFERENCES `lista_lugares`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_lista_items_productoId` ON `lista_items` (`productoId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_lista_items_lugarId` ON `lista_items` (`lugarId`)")

        // Insertar lugares (Barcelona = default)
        database.execSQL("INSERT INTO lista_lugares (nombre, esDefault) VALUES ('Barcelona', 1)")
        database.execSQL("INSERT INTO lista_lugares (nombre, esDefault) VALUES ('Madrid', 0)")
        database.execSQL("INSERT INTO lista_lugares (nombre, esDefault) VALUES ('Menorca', 0)")

        // Insertar categorías de producto (orden 1-14)
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Lácteos', '#FF9800', 1)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Bebidas', '#2196F3', 2)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Frutas', '#4CAF50', 3)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Verduras', '#8BC34A', 4)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Carnes y Pescados', '#F44336', 5)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Panadería', '#FF5722', 6)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Congelados', '#00BCD4', 7)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Pastas y Arroces', '#FFC107', 8)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Conservas', '#795548', 9)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Limpieza', '#9C27B0', 10)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Higiene', '#E91E63', 11)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Desayuno y Cereales', '#FF6F00', 12)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Snacks', '#9E9E9E', 13)")
        database.execSQL("INSERT INTO lista_categorias_producto (nombre, colorHex, orden) VALUES ('Otros', '#607D8B', 14)")

        // Insertar productos — Lácteos (cat 1)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Leche entera', 1), ('Leche semidesnatada', 1), ('Leche desnatada', 1),
            ('Nata para cocinar', 1), ('Nata para montar', 1), ('Mantequilla', 1),
            ('Mantequilla sin sal', 1), ('Queso manchego', 1), ('Queso fresco', 1),
            ('Queso brie', 1), ('Queso gouda', 1), ('Queso emmental', 1),
            ('Queso parmesano', 1), ('Queso roquefort', 1), ('Yogur natural', 1),
            ('Yogur griego', 1), ('Yogur de fresa', 1), ('Yogur de limón', 1),
            ('Kéfir', 1), ('Batido de chocolate', 1), ('Batido de vainilla', 1),
            ('Queso crema', 1), ('Requesón', 1), ('Leche condensada', 1),
            ('Leche evaporada', 1)
        """.trimIndent())

        // Bebidas (cat 2)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Agua mineral', 2), ('Agua con gas', 2), ('Zumo de naranja', 2),
            ('Zumo de manzana', 2), ('Zumo de piña', 2), ('Zumo de tomate', 2),
            ('Refresco de cola', 2), ('Refresco de limón', 2), ('Cerveza', 2),
            ('Cerveza sin alcohol', 2), ('Vino tinto', 2), ('Vino blanco', 2),
            ('Vino rosado', 2), ('Cava', 2), ('Sidra', 2), ('Tónica', 2),
            ('Naranjada', 2), ('Limonada', 2), ('Té frío', 2), ('Café molido', 2),
            ('Café en grano', 2), ('Té verde', 2), ('Manzanilla', 2),
            ('Infusión de menta', 2)
        """.trimIndent())

        // Frutas (cat 3)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Manzana', 3), ('Pera', 3), ('Naranja', 3), ('Mandarina', 3),
            ('Limón', 3), ('Pomelo', 3), ('Uvas', 3), ('Fresas', 3),
            ('Frambuesas', 3), ('Arándanos', 3), ('Kiwi', 3), ('Mango', 3),
            ('Piña', 3), ('Sandía', 3), ('Melón', 3), ('Melocotón', 3),
            ('Nectarina', 3), ('Ciruela', 3), ('Cereza', 3), ('Albaricoque', 3),
            ('Plátano', 3), ('Higo', 3), ('Granada', 3), ('Coco', 3),
            ('Aguacate', 3)
        """.trimIndent())

        // Verduras (cat 4)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Tomate', 4), ('Lechuga', 4), ('Cebolla', 4), ('Ajo', 4),
            ('Pimiento rojo', 4), ('Pimiento verde', 4), ('Pimiento amarillo', 4),
            ('Zanahoria', 4), ('Patata', 4), ('Boniato', 4), ('Pepino', 4),
            ('Calabacín', 4), ('Berenjena', 4), ('Espinacas', 4), ('Acelgas', 4),
            ('Brócoli', 4), ('Coliflor', 4), ('Col', 4), ('Puerro', 4),
            ('Apio', 4), ('Champiñones', 4), ('Espárragos', 4), ('Alcachofas', 4),
            ('Guisantes frescos', 4), ('Habas', 4)
        """.trimIndent())

        // Carnes y Pescados (cat 5)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Pechuga de pollo', 5), ('Muslos de pollo', 5), ('Pollo entero', 5),
            ('Ternera picada', 5), ('Filete de ternera', 5), ('Lomo de cerdo', 5),
            ('Costillas de cerdo', 5), ('Chuletas de cerdo', 5), ('Jamón york', 5),
            ('Jamón serrano', 5), ('Salchichas', 5), ('Chorizo', 5),
            ('Morcilla', 5), ('Bacalao', 5), ('Salmón', 5), ('Atún fresco', 5),
            ('Lubina', 5), ('Dorada', 5), ('Merluza', 5), ('Gambas', 5),
            ('Langostinos', 5), ('Mejillones', 5), ('Almejas', 5),
            ('Calamares', 5)
        """.trimIndent())

        // Panadería (cat 6)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Pan de molde', 6), ('Barra de pan', 6), ('Pan integral', 6),
            ('Pan de centeno', 6), ('Pan de pita', 6), ('Croissant', 6),
            ('Magdalenas', 6), ('Donuts', 6), ('Bizcocho', 6),
            ('Galletas María', 6), ('Galletas integrales', 6), ('Tostadas', 6)
        """.trimIndent())

        // Congelados (cat 7)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Guisantes congelados', 7), ('Espinacas congeladas', 7),
            ('Brócoli congelado', 7), ('Pizza margarita', 7), ('Pizza 4 quesos', 7),
            ('Croquetas', 7), ('Empanadillas', 7), ('Nuggets de pollo', 7),
            ('Bastones de merluza', 7), ('Gambas congeladas', 7),
            ('Patatas fritas congeladas', 7), ('Helado de vainilla', 7),
            ('Helado de chocolate', 7)
        """.trimIndent())

        // Pastas y Arroces (cat 8)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Espaguetis', 8), ('Macarrones', 8), ('Penne', 8), ('Fusilli', 8),
            ('Tallarines', 8), ('Arroz blanco', 8), ('Arroz integral', 8),
            ('Arroz para sushi', 8), ('Pasta fresca', 8), ('Lasaña', 8),
            ('Fideos', 8), ('Cuscús', 8), ('Quinoa', 8), ('Lentejas', 8),
            ('Garbanzos', 8), ('Alubias', 8)
        """.trimIndent())

        // Conservas (cat 9)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Atún en lata', 9), ('Sardinas en lata', 9), ('Mejillones en lata', 9),
            ('Berberechos en lata', 9), ('Anchoas', 9), ('Tomate triturado', 9),
            ('Tomate frito', 9), ('Pimientos del piquillo', 9), ('Maíz en lata', 9),
            ('Aceitunas verdes', 9), ('Aceitunas negras', 9), ('Pepinillos', 9),
            ('Alcaparras', 9), ('Judías verdes en lata', 9), ('Caldo de pollo', 9),
            ('Caldo de verduras', 9)
        """.trimIndent())

        // Limpieza (cat 10)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Detergente lavadora', 10), ('Suavizante', 10), ('Lavavajillas', 10),
            ('Bayetas', 10), ('Estropajo', 10), ('Fregona', 10), ('Mopa', 10),
            ('Lejía', 10), ('Quitamanchas', 10), ('Limpiacristales', 10),
            ('Multiusos', 10), ('Desinfectante', 10), ('Bolsas de basura', 10),
            ('Papel de cocina', 10), ('Papel de aluminio', 10),
            ('Film transparente', 10)
        """.trimIndent())

        // Higiene (cat 11)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Champú', 11), ('Acondicionador', 11), ('Gel de ducha', 11),
            ('Jabón de manos', 11), ('Pasta de dientes', 11), ('Cepillo de dientes', 11),
            ('Hilo dental', 11), ('Enjuague bucal', 11), ('Desodorante', 11),
            ('Crema hidratante', 11), ('Crema facial', 11), ('Protector solar', 11),
            ('Maquinilla de afeitar', 11), ('Espuma de afeitar', 11),
            ('Compresas', 11), ('Tampones', 11), ('Papel higiénico', 11),
            ('Pañuelos', 11), ('Algodón', 11), ('Alcohol', 11)
        """.trimIndent())

        // Desayuno y Cereales (cat 12)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Cereales corn flakes', 12), ('Cereales con miel', 12), ('Muesli', 12),
            ('Avena', 12), ('Granola', 12), ('Cacao soluble', 12), ('Cola Cao', 12),
            ('Nesquik', 12), ('Mermelada de fresa', 12), ('Mermelada de naranja', 12),
            ('Miel', 12), ('Azúcar', 12), ('Azúcar moreno', 12),
            ('Edulcorante', 12), ('Cacao en polvo', 12), ('Nutella', 12)
        """.trimIndent())

        // Snacks (cat 13)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Patatas fritas', 13), ('Nachos', 13), ('Palomitas', 13),
            ('Pipas', 13), ('Almendras', 13), ('Nueces', 13), ('Cacahuetes', 13),
            ('Anacardos', 13), ('Pistachos', 13), ('Barritas de cereales', 13),
            ('Chocolate negro', 13), ('Chocolate con leche', 13),
            ('Chocolate blanco', 13), ('Galletas oreo', 13), ('Gominolas', 13),
            ('Caramelos', 13), ('Chicles', 13)
        """.trimIndent())

        // Otros (cat 14)
        database.execSQL("""INSERT INTO lista_productos (nombre, categoriaId) VALUES
            ('Aceite de oliva virgen extra', 14), ('Aceite de girasol', 14),
            ('Vinagre de vino', 14), ('Vinagre de manzana', 14), ('Sal', 14),
            ('Pimienta negra', 14), ('Pimentón dulce', 14), ('Pimentón picante', 14),
            ('Comino', 14), ('Orégano', 14), ('Albahaca', 14), ('Tomillo', 14),
            ('Laurel', 14), ('Curry', 14), ('Canela', 14), ('Vainilla', 14),
            ('Levadura', 14), ('Harina de trigo', 14), ('Harina integral', 14),
            ('Maicena', 14), ('Pan rallado', 14), ('Huevos', 14),
            ('Mayonesa', 14), ('Ketchup', 14), ('Mostaza', 14)
        """.trimIndent())
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Campos de Límite Máximo en habitos
        database.execSQL("ALTER TABLE habitos ADD COLUMN limiteMaximo REAL DEFAULT NULL")
        database.execSQL("ALTER TABLE habitos ADD COLUMN tramosLimite TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE habitos ADD COLUMN ubeActivo INTEGER NOT NULL DEFAULT 0")
        // Valor decimal en historial (para hábitos de límite que registran decimales)
        database.execSQL("ALTER TABLE habitos_historial ADD COLUMN valorProgresoDecimal REAL NOT NULL DEFAULT 0.0")
        // Campos de Límite Máximo en versiones
        database.execSQL("ALTER TABLE habitos_versiones ADD COLUMN limiteMaximo REAL DEFAULT NULL")
        database.execSQL("ALTER TABLE habitos_versiones ADD COLUMN tramosLimite TEXT DEFAULT NULL")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos_categorias ADD COLUMN orden INTEGER NOT NULL DEFAULT 0")
        // Orden inicial: 1=Aprendizaje, 2=Salud, 3=Bienestar, 4=Ejercicio/Deporte
        database.execSQL("UPDATE habitos_categorias SET orden = 1 WHERE LOWER(nombre) = 'aprendizaje'")
        database.execSQL("UPDATE habitos_categorias SET orden = 2 WHERE LOWER(nombre) = 'salud'")
        database.execSQL("UPDATE habitos_categorias SET orden = 3 WHERE LOWER(nombre) = 'bienestar'")
        database.execSQL("UPDATE habitos_categorias SET orden = 4 WHERE LOWER(nombre) IN ('ejercicio','deporte')")
    }
}

@TypeConverters(Converters::class)
@Database(
    entities = [
        Tarea::class, Categoria::class,
        Habito::class, HabitoHistorial::class, CategoriaHabito::class, TareaHabito::class,
        TareaHabitoHistorial::class, HabitoVersion::class, HabitoPausa::class,
        ListaLugar::class, ListaCategoriaProducto::class, ListaProducto::class, ListaItem::class
    ],
    version = 23,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun habitoDao(): HabitoDao
    abstract fun listaCompraDao(): ListaCompraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tareas_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23)
                    .addCallback(DatabaseCallback(context))
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        fun resetearInstancia() {
            INSTANCE = null
        }
    }

    // Dentro de AppDatabase.kt -> DatabaseCallback
    private class DatabaseCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val habitoDao = database.habitoDao()

                    // 1. Insertar Categorías de Hábitos
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Salud", icono = "medical_services", color = "#EF5350"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Deporte", icono = "fitness_center", color = "#66BB6A"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Aprendizaje", icono = "school", color = "#42A5F5"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Bienestar", icono = "self_improvement", color = "#AB47BC"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Personal", icono = "person", color = "#FFA726"))

                    // 2. Rellenar Hábitos de la imagen
                    habitoDao.insertarHabito(Habito(
                        nombre = "Ingles",
                        categoriaId = 3, // Aprendizaje
                        frecuencia = FrecuenciaHabito.SEMANAL,
                        objetivoValor = 100,
                        unidad = "Minutos",
                        icono = "language",
                        colorHex = "#42A5F5"
                    ))

                    habitoDao.insertarHabito(Habito(
                        nombre = "Bisoprolol",
                        categoriaId = 1, // Salud
                        frecuencia = FrecuenciaHabito.DIARIA,
                        objetivoValor = 1,
                        unidad = "vez",
                        icono = "medication",
                        colorHex = "#EF5350"
                    ))

                    habitoDao.insertarHabito(Habito(
                        nombre = "Pasos",
                        categoriaId = 2, // Deporte
                        frecuencia = FrecuenciaHabito.SEMANAL,
                        objetivoValor = 40000,
                        unidad = "Pasos",
                        icono = "directions_run",
                        colorHex = "#66BB6A"
                    ))

                    habitoDao.insertarHabito(Habito(
                        nombre = "Día sin Alcohol",
                        categoriaId = 4, // Bienestar
                        frecuencia = FrecuenciaHabito.MENSUAL,
                        objetivoValor = 18, // 60% de 30 días aprox
                        unidad = "días",
                        icono = "no_drinks",
                        colorHex = "#AB47BC"
                    ))
                }
            }
        }
    }
}