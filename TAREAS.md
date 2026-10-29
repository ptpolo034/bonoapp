# Tareas de BonoApp

<!-- AVANCE:INICIO -->
**Avance general: ██████░░░░ 66%**

| Tarea | Avance |
|---|---|
| Por hacer (para la próxima versión) | ░░░░░░░░░░ 0% |
| Hecho para la versión 1.0-prueba2 (todavía sin entregar) | ██████████ 100% |
| Hecho en la versión 1.0-prueba1 | ██████████ 100% |
<!-- AVANCE:FIN -->

Lista viva: se agrega aquí todo lo que salga mientras se prueba el programa (errores, ajustes, ideas del dueño o la dueña).
Versión en uso en el computador de uso: **1.0-prueba1** (entregada el 2026-10-20). La **1.0-prueba2** (instalador 1.0.3) ya está lista para instalar.
Los cambios nuevos que salgan de la prueba se juntan para la siguiente versión.

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso

El porcentaje de cada sección se calcula solo con las casillas marcadas; las que están en curso todavía no suman.

---

## Por hacer (para la próxima versión) (0%)
- [ ] **Instalar `BonoApp-1.0.3.exe` en el computador de uso** (`instalador\salida`; Windows mostrará el aviso de "protegió su PC" porque no está firmado). Antes, cerrar BonoApp.
  *Por confirmar:* si se agrega un contacto (correo o teléfono) junto a la firma; por ahora, solo el nombre.

## Descartadas (decidido el 2026-10-27, no se hacen)
- Entrega más rápida: repetir la entrega de ayer o pegar una lista de números.
- Premio mayor: anotar si el premio se pagó o si el ganador cobró.
- Cifrar el documento y el teléfono de las vendedoras.

## Errores encontrados durante la prueba

*(todavía ninguno reportado; cada error nuevo se agrega aquí como una casilla `- [ ]`)*

## Hecho para la versión 1.0-prueba2 (todavía sin entregar) (100%)
- [x] **Pruebas automáticas dentro del proyecto** (JUnit, `mvn test`): 26 pruebas de las reglas del negocio (tratos por tipo y migración, números repetidos, lote de 2.000, mover/editar, liquidaciones, ganador, ahorros, préstamos, historial, copias de seguridad). Corren en una carpeta propia, sin tocar la base real. Se comprobó que fallan si se rompe una regla.
- [x] **Crédito en el instalador:** el instalador muestra como empresa "Henry Polo" y derechos "© 2026 Henry Polo" (comprobado en las propiedades del archivo).
- [x] **Guía de "primeros pasos"** en Inicio mientras no haya ninguna boleta registrada: 4 pasos (copias de seguridad, precios, vendedoras, primera entrega) con marca verde en los que ya están hechos y un botón que lleva a cada pantalla. Desaparece sola al registrar la primera entrega.
- [x] **Versión 1.0-prueba2 lista:** instalador `BonoApp-1.0.3.exe` generado (el número del instalador es 1.0.3; el programa se llama 1.0-prueba2); la actualización encima de la 1.0.1 se probó (datos conservados, tratos migrados). El instalador anterior quedó en `instalador\salida\anteriores`.
- [x] **Tratos independientes por tipo:** la Diaria y el Premio mayor tienen cada una su lista de tratos por vendedora.
  En Registrar entrega, al cambiar de tipo se muestran los tratos de ese tipo. Los tratos que ya existían se copiaron una sola vez a los dos tipos, para no perder ninguno.
- [x] **Total de boletas en la pantalla de Boletas:** por tipo y para el día que se ve, "1.250 de 2.000 · Faltan 750", barra de progreso, "Completo" al llegar a 2.000 y cuántas van sin número.
- [x] **Firma "Desarrollado por Henry Polo"** en el inicio de sesión y en Configuración → Soporte (versión · firma · © 2026).
- [x] **Firma también en el menú lateral** (abajo, pequeña y en gris suave: versión y "Desarrollado por Henry Polo"), para que se vea en todas las pantallas.

## Hecho en la versión 1.0-prueba1 (100%)
- [x] Copias de seguridad automáticas (carpeta elegida al inicio, una por día, últimas 14).
- [x] Boletas por día, con búsqueda en todos los días; cierre del día en Inicio.
- [x] Liquidación con marcado rápido de devueltas y "mover a otra vendedora"; liquidaciones reversibles.
- [x] Registro de errores (`errores.log`) y versión visible en Configuración → Soporte.
- [x] Instalador para Windows 10/11 (64 bits) para un solo usuario.
