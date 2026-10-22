# Tareas de BonoApp

<!-- AVANCE:INICIO -->
**Avance general: ███░░░░░░░ 33%**

| Tarea | Avance |
|---|---|
| Por hacer (para la próxima versión) | ░░░░░░░░░░ 0% |
| Ideas ya acordadas, sin hacer | ░░░░░░░░░░ 0% |
| Hecho en la versión 1.0-prueba1 | ██████████ 100% |
<!-- AVANCE:FIN -->

Lista viva: se agrega aquí todo lo que salga mientras se prueba el programa (errores, ajustes, ideas del dueño o la dueña).
Versión instalada actualmente: **1.0-prueba1** (entregada el 2026-10-20). Mientras se prueba no se cambia el código;
las tareas se hacen juntas en la siguiente versión (**1.0-prueba2**).

**Leyenda:** `[x]` hecho · `[ ]` pendiente · `[~]` en curso

El porcentaje de cada sección se calcula solo con las casillas marcadas; las que están en curso todavía no suman.

---

## Por hacer (para la próxima versión) (0%)
- [ ] **Total de boletas en la pantalla de Boletas.**
  Mientras se entregan las boletas a las vendedoras, el dueño o la dueña quiere ver que se entregaron las 2.000, tanto de la
  Diaria como del Premio mayor. Debe actualizarse solo después de cada "Registrar entrega", por tipo y para el día que se
  está viendo (por ejemplo "Diaria: 1.250 de 2.000, faltan 750"), y marcarse como **completo** al llegar a 2.000.
  Solo en la pantalla de Boletas (no en Inicio).
  *Por confirmar:* barra de progreso; mostrar cuántas boletas se registraron sin número todavía.
- [ ] **Firma / crédito del desarrollador en el programa** ("Desarrollado por Henry Polo").
  Hoy el programa no dice quién lo desarrolló. **Decidido:** el texto estándar **"Desarrollado por Henry Polo"**, en tres lugares:
  - pantalla de inicio de sesión (línea pequeña, gris suave, abajo);
  - Configuración → "Acerca de" (versión · Desarrollado por Henry Polo · © 2026);
  - datos del instalador y de Windows (publicador "Henry Polo", derechos "© 2026 Henry Polo").

  *Por confirmar:* si se agrega un contacto (correo o teléfono); por ahora, solo el nombre.

## Ideas ya acordadas, sin hacer (0%)
- [ ] **Entrega más rápida:** "repetir la entrega de ayer" o pegar una lista de números (depende de cómo le llegan los números).
- [ ] **Guía de "primeros pasos"** en Inicio cuando todo está vacío.
- [ ] **Premio mayor:** anotar si el premio se pagó o si el ganador cobró.
- [ ] **Pruebas automáticas dentro del proyecto**, para proteger los cambios futuros.
- [ ] **Cifrar** el documento y el teléfono de las vendedoras (riesgo bajo).

## Errores encontrados durante la prueba

*(todavía ninguno reportado; cada error nuevo se agrega aquí como una casilla `- [ ]`)*

## Hecho en la versión 1.0-prueba1 (100%)
- [x] Copias de seguridad automáticas (carpeta elegida al inicio, una por día, últimas 14).
- [x] Boletas por día, con búsqueda en todos los días; cierre del día en Inicio.
- [x] Liquidación con marcado rápido de devueltas y "mover a otra vendedora"; liquidaciones reversibles.
- [x] Registro de errores (`errores.log`) y versión visible en Configuración → Soporte.
- [x] Instalador para Windows 10/11 (64 bits) para un solo usuario.
