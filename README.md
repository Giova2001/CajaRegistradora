# Tienda Local — Android Java + SQLite

Aplicación Android nativa con tres pestañas: Venta, Historial e Inventario.

## Abrir
1. Android Studio > Open > seleccionar carpeta `TiendaLocal`.
2. Instalar Android SDK 35 y JDK 17; sincronizar Gradle (requiere internet inicialmente para dependencias).
3. Ejecutar en Android 8.0+ (API 26). Se recomienda Android 10+.
4. Aceptar el permiso de cámara para escanear códigos. También se permite introducirlos manualmente.

## Implementado
- SQLite local `mysql.db` con claves foráneas.
- CRUD de productos (archivado en vez de borrado), bienes y etiquetas.
- Asociación de etiquetas a productos; stock mínimo; campo de vencimiento.
- Escáner ZXing, ingreso manual de código, cantidad y carrito.
- Confirmación de venta atómica y descuento de stock.
- Historial, detalle de ventas, resumen diario, reportes textuales agrupados por períodos.
- Importación/exportación de productos CSV, exportación de ventas CSV.
- Respaldo SQLite consistente y restauración con validación básica.

## Pendiente para producción
- Las gráficas son reportes textuales; reemplazar por gráficos Android reales.
- Las razones financieras no se calculan aún; las tablas contables están creadas como base.
- Lotes por fecha de vencimiento: tabla creada, pendiente formulario y asignación en ventas.
- La importación CSV se realiza fila a fila; no es transaccional. Agregar vista previa, validación completa y rollback en lote.
- Restauración: fortalecer compatibilidad por versión, recuperación automática si falla la sustitución y protección cifrada del respaldo.
- Mejorar interfaz visual con componentes Material, filtros, paginación y pruebas automatizadas.
- No incluye APK compilado ni Gradle Wrapper (Android Studio puede sincronizar con Gradle instalado).

## Seguridad y consistencia
Las ventas se guardan en una transacción SQLite. La base vive en almacenamiento privado de la app. No habilitar `allowBackup` para evitar respaldos automáticos sin control. No copiar el archivo SQLite abierto directamente; se usa `VACUUM INTO` para exportarlo.
