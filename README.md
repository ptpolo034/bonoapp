# BonoApp

Software de escritorio en Java (Swing + FlatLaf + SQLite) para manejar un negocio de boletas de sorteo:
vendedoras, entrega y liquidación de boletas (Diaria y Premio mayor), ahorros, préstamos con abonos,
reportes, historial de cambios y copia de seguridad. Funciona sin internet y guarda todo en un solo archivo.

## Requisitos
- JDK 17 o superior (el proyecto se mantiene en Java 17 a propósito: no subir la versión en el `pom.xml`)
- Maven (o abrir el proyecto en NetBeans / IntelliJ, que ya lo incluyen)

## Compilar y ejecutar
    mvn -q package
    java -jar target/bonoapp-1.0.jar

Si la compilación falla con "Failed to delete target\classes", es el servidor de Java de VS Code que tiene la
carpeta abierta: no hace falta cerrarlo, basta con volver a ejecutar `mvn -q package`.

## Reglas del negocio
- Dos tipos de boleta: Diaria y Premio mayor, cada uno con su precio (Configuración).
- Los lotes son por día y por tipo: máximo 2.000 boletas por tipo por fecha de entrega, con números de 3 cifras
  y cada número hasta 2 veces por día y por tipo. Los números de días anteriores no cuentan.
- Las vendedoras se quedan con su ganancia y entregan solo el valor de la boleta: lo recaudado es
  vendidas × precio. No hay comisión; el ahorro se registra a mano.
- Cada liquidación queda guardada (con el precio de ese momento) y se puede revertir.

## Estructura (`src/main/java/com/bonoapp`)
- `Main.java`      arranque: base de datos, tema, inicio de sesión y ventana principal
- `db/`            acceso a datos (SQLite en `~/.bonoapp/bonoapp.db`); un DAO por tabla, con las reglas del negocio
- `model/`         clases de datos (vendedoras, boletas, liquidaciones, ahorros, préstamos...)
- `ui/`            pantallas, diálogos y componentes visuales compartidos (tema, tablas, botones, etiquetas)
- `util/`          preferencias (`~/.bonoapp/config.properties`), copia de seguridad y sesión
- `src/main/resources/icons`  íconos SVG (Tabler Icons, licencia MIT)

## Convenciones
- Los cambios que modifican datos se anotan en el historial (`HistorialDAO`) dentro de la misma transacción.
- La interfaz usa `Theme` (colores), `Botones`, `Tablas`, `Chip` y `Formularios` para mantener un estilo único.
- Para probar la lógica sin tocar los datos reales, ejecutar con `-Duser.home=<carpeta temporal>`.

## Instalador (Windows 10/11, 64 bits)

El instalador se genera con `instalador\crear_instalador.ps1` (ver las instrucciones dentro del script) y queda en `instalador\salida`. Lleva su propio Java incluido, se instala solo para el usuario que lo ejecuta (sin permisos de administrador) y no borra los datos al actualizar o desinstalar (viven en `%USERPROFILE%\.bonoapp`). Requiere WiX Toolset 3.x en el equipo que lo genera. Para sacar una version nueva: cambiar `util/Version.java`, compilar, y ejecutar el script con `-VersionInstalador` mayor (por ejemplo 1.0.2). No cambiar el UUID de actualizacion del script.
