# Investigación y fuentes

Consultadas el **2026-10-01**. Cuando dos fuentes difieren, prevalece la oficial y más reciente.

| # | Fuente (oficial / confiable) | URL | Consultada | Qué se tomó de aquí |
|---|---|---|---|---|
| 1 | Spring — generaciones y soporte de Spring Boot | https://api.spring.io/projects/spring-boot/generations | 2026-10-01 | 3.5.x: fin de soporte OSS el **2026-06-30**. 4.1.x: OSS hasta 2027-07-31. Se usa **Spring Boot 4.1.1** (GA actual) en lugar de 3.x |
| 2 | Spring Boot 4.1.1 — requisitos del sistema | https://docs.spring.io/spring-boot/system-requirements.html | 2026-10-01 | Java 17 a 26 (se usa Java 21 LTS), Spring Framework 7.0.9+, Tomcat 11, Maven 3.6.3+ |
| 3 | Spring Modulith 2.1 GA (blog oficial) | https://spring.io/blog/2026/06/11/spring-modulith-2-1-ga-2-0-7-and-1-4-12-released/ | 2026-10-01 | Modulith 2.1 se alinea con Boot 4.1. Se usa 2.1.1 para verificar los límites del monolito modular (`ApplicationModules.verify()`) |
| 4 | Spring Framework — STOMP, external broker | https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-broker-relay.html | 2026-10-01 | El *broker relay* exige un broker STOMP completo (RabbitMQ/ActiveMQ). Redis no lo es, así que cada instancia usa su *simple broker* y se reenvían los eventos por Redis Pub/Sub |
| 5 | Spring Security 7.1 — WebSocket | https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html | 2026-10-01 | El navegador no aplica la política de mismo origen a WebSocket → lista explícita de `Origin`. Autorización por mensaje y `denyAll` para lo no previsto |
| 6 | OWASP WebSocket Security Cheat Sheet | https://cheatsheetseries.owasp.org/cheatsheets/WebSocket_Security_Cheat_Sheet.html | 2026-10-01 | Validar `Origin` (CSWSH), autenticar con token, autorizar cada acción, limitar el tamaño de los mensajes, usar heartbeats y no registrar tokens |
| 7 | OWASP API Security Top 10 (2023) | https://owasp.org/API-Security/editions/2023/en/0x11-t10/ | 2026-10-01 | API1 BOLA (token atado a un PIN), API3 exposición de propiedades (la respuesta nunca viaja con la pregunta abierta), API4/API6 rate limiting contra la enumeración de PIN |
| 8 | STOMP 1.2 | https://stomp.github.io/stomp-specification-1.2.html | 2026-10-01 | Negociación de heart-beat; el servidor cierra la conexión tras enviar un frame `ERROR` |
| 9 | Redis — Pub/Sub, semántica de entrega | https://redis.io/docs/latest/develop/pubsub/ | 2026-10-01 | Entrega *at-most-once*: un suscriptor desconectado pierde mensajes → `seq` + snapshot al reconectar |
| 10 | Redis — licencias | https://redis.io/legal/licenses/ | 2026-10-01 | Redis 8.x: triple licencia RSALv2 / SSPLv1 / AGPLv3. Uso como servicio interno sin modificar → sin obligaciones extra |
| 11 | Docker Hub — etiquetas oficiales | https://hub.docker.com/_/redis · https://hub.docker.com/_/postgres · https://hub.docker.com/_/eclipse-temurin · https://hub.docker.com/_/node | 2026-10-01 | Imágenes fijadas: `postgres:18.6-alpine`, `redis:8.8.3-alpine`, `eclipse-temurin:21.0.12.1_1-jdk/jre-alpine`, `node:24.21.0-alpine`, `nginxinc/nginx-unprivileged:1.29.8-alpine` |
| 12 | PostgreSQL — política de versiones | https://www.postgresql.org/support/versioning/ | 2026-10-01 | PostgreSQL 18 (18.6) estable, soportado hasta 2030-11-14 |
| 13 | Flyway (Redgate) — PostgreSQL | https://documentation.red-gate.com/fd/postgresql-database-277579325.html | 2026-10-01 | PostgreSQL 18 verificado; requiere `flyway-database-postgresql` |
| 14 | Angular — versiones y compatibilidad | https://angular.dev/reference/releases · https://angular.dev/reference/versions | 2026-10-01 | Angular 22 activo (desde 2026-06-03); requiere Node `^22.22.3 \|\| ^24.15.0 \|\| ^26`, TypeScript 6.0 |
| 15 | Node.js — calendario de releases | https://github.com/nodejs/Release/blob/main/schedule.json | 2026-10-01 | Node 24 LTS (fin 2028-04-30). Se usa en Docker y CI. Node 25 (el local) es impar y ya llegó a EOL |
| 16 | Tailwind CSS — guía para Angular | https://tailwindcss.com/docs/installation/framework-guides/angular | 2026-10-01 | Tailwind 4 vía `@tailwindcss/postcss` + `.postcssrc.json` + `@import "tailwindcss"` |
| 17 | npm registry — @stomp/stompjs, @stomp/rx-stomp | https://registry.npmjs.org/@stomp/stompjs/latest · https://registry.npmjs.org/@stomp/rx-stomp/latest | 2026-10-01 | stompjs 7.3.0 y rx-stomp 2.4.0 (Apache-2.0); reconexión con backoff exponencial (`reconnectTimeMode`) |
| 18 | Maven Central | https://repo1.maven.org/maven2/ | 2026-10-01 | springdoc-openapi 3.1.1 (Boot 4), commons-csv 1.14.1, jacoco 0.8.15, Testcontainers 2.0.x (gestionado por Boot) |
| 19 | RFC 7518 §3.2 (JWA, HS256) | https://www.rfc-editor.org/rfc/rfc7518#section-3.2 | 2026-10-01 | La clave HMAC debe tener al menos el tamaño del hash → `JWT_SECRET` de 32 bytes o más; la app no arranca si es menor |
| 20 | RFC 4180 (CSV) | https://www.rfc-editor.org/rfc/rfc4180 | 2026-10-01 | Formato de importación CSV (comillas, comas dentro de campos) |
| 21 | Render — plan gratuito | https://render.com/docs/free | 2026-10-01 | El servicio web se duerme tras 15 min sin tráfico entrante, y los mensajes WebSocket cuentan como tráfico. Key Value gratis sin persistencia: se pierde al reiniciar |
| 22 | Render — WebSockets | https://render.com/docs/websocket | 2026-10-01 | WebSockets soportados. Las conexiones se cierran en cada deploy → el cliente debe reconectar con backoff |
| 23 | Upstash Redis — precios | https://upstash.com/pricing/redis | 2026-10-01 | Gratis: 500K comandos/mes. El sondeo de deadlines (2 comandos/s) lo agotaría → no se recomienda para este proyecto |
| 24 | Kahoot! — Guía de la API de reportes | https://support.kahoot.com/hc/en-us/articles/11735948502931 | 2026-10-01 | API REST solo para clientes del plan más alto, OAuth2 *client credentials*, datos **después** de terminar la partida (90 días). **No existe API oficial de partidas en vivo** |
| 25 | Kahoot! — Política de uso aceptable (2022-08-10) | https://trust.kahoot.com/acceptable-use-policy/ | 2026-10-01 | Prohíbe acceder a los servicios por cualquier medio distinto al provisto o por medios automatizados → no se implementa ningún conector no autorizado |
| 26 | LOPDP Ecuador (R.O. 5.º Supl. 459, 26-may-2021) | https://www.gob.ec/regulaciones/ley-organica-proteccion-datos-personales-0 | 2026-10-01 | Minimización: los viewers son anónimos (sin nombre, correo ni cédula). Solo se guarda el correo del admin |

## Decisiones derivadas

- **Spring Boot 4.1.1 en lugar de "Spring Boot 3"** (#1): la línea 3.x ya no recibe parches OSS. Java se mantiene en 21 (LTS) como se pidió.
- **Redis como bus entre instancias + estado efímero**, no como broker STOMP (#4, #9). PostgreSQL es la fuente de verdad, y lo que vive en Redis se puede reconstruir.
- **El visor es de solo lectura** (cambio de alcance del 2026-10-01): se eliminó `sendAnswer()` y con ello las tablas `players` y `answers` y el módulo `answers`. El protocolo STOMP rechaza cualquier `SEND`.
- **Proveedor externo** (#24, #25): `QuizProvider` queda como puerto. Hoy un `AuthorizedKahootProvider` solo podría usar la API de *reportes* (post-partida), así que no se crea esa clase: sería código muerto. Ver [arquitectura.md](arquitectura.md#proveedores).

## Supuestos (no verificados)

- **SUPUESTO:** los límites de Neon (0,5 GB, 100 CU-h) son los indicados en las reglas del portafolio (sep-2026); no se volvieron a consultar.
- **SUPUESTO:** la memoria exacta de Render Key Value gratis no aparece en la documentación consultada (#21); los datos de este proyecto en Redis ocupan pocos KB por partida.
- **Riesgo de marca:** "Kahoot!" es marca de Kahoot! ASA. El nombre del repositorio la incluye por decisión del autor; el README aclara que el proyecto no está afiliado.
