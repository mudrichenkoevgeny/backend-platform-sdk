# Планируемые изменения для поддержки балансировщика и stateless-архитектуры

В данном плане описаны доработки Backend SDK для развертывания за Nginx балансировщиком и корректной работы множества Ktor-инстансов.
Цель: обеспечение проксирования заголовков, добавление Health-чеков (Liveness/Readiness), настройка Graceful Shutdown и перевод WebSockets на честную Stateless модель (Pub/Sub).

## User Review Required

> [!WARNING]
> **Stateless WebSockets & Redis Pub/Sub**
> В данный момент `KtorWebSocketManager` хранит список подключенных сессий в локальной памяти (в `ConcurrentHashMap`). В кластерной среде с Nginx событие, инициированное на одном инстансе, может предназначаться пользователю, который подключен по WebSocket к другому инстансу.
> **Требуется подтверждение**: план включает переход на отправку WebSocket-уведомлений через Redis Pub/Sub. При отправке сообщения инстанс будет публиковать его в Redis, а каждый узел (получив сообщение) доставлять его локальным клиентам, если они подключены. Согласны ли вы на такое усложнение WebSocket слоя?

## Open Questions

- **Graceful Shutdown timeouts**: По умолчанию используются настройки Netty для Graceful Shutdown. Обычно `shutdownGracePeriod = 2-5` сек, и `shutdownTimeout = 5-10` сек. Есть ли строгие требования по тайм-аутам для балансировщика Nginx?
- **Health Checks**: Стоит ли разделять `/health/live` (приложение просто запущено) и `/health/ready` (проверка DB/Redis)? По плану мы сделаем два отдельных эндпоинта.

## Proposed Changes

### `core/common` (Application, Server, Routing)

Изменения для поддержки проксирования заголовков и Graceful Shutdown сервера Ktor. Определение Health Router'а.

#### [MODIFY] `ApplicationHTTPConfiguration.kt`
- Добавление `install(ForwardedHeaders)` и `install(XForwardedHeaders)` для корректного чтения реальных IP адресов и протоколов клиентов при работе за Nginx-прокси.

#### [MODIFY] `KtorServerFactory.kt`
- В `embeddedServer(Netty)` добавится блок конфигурации с тайм-аутами `shutdownGracePeriod` и `shutdownTimeout`.
- Регистрация слушателя событий `environment.monitor.subscribe(ApplicationStopping)` для плавной остановки внутренних сервисов.

#### [MODIFY] `HealthCheckerManager.kt`
- Реализация suspended метода `checkCriticalHealthAsync(): AppSystemResult<Unit>`, не блокирующего поток (текущий метод `verifyCriticalHealth` использует `runBlocking`, что не подходит для обслуживания HTTP-запросов).

#### [NEW] `ManagementHealthRouter.kt`
- Создание Ktor маршрута, который работает только на `ktorManagementPort` (через существующий `onPort`).
- Эндпоинт `GET /health/live`: Возвращает 200 OK всегда (Ktor жив).
- Эндпоинт `GET /health/ready`: Вызывает `checkCriticalHealthAsync()` из `HealthCheckerManager` и возвращает `200 OK`, если базы данных доступны, иначе `503 Service Unavailable`.

---

### `core/database` (Data Sources)

Изменения для корректного закрытия пулов коннектов при остановке приложения (Graceful Shutdown).

#### [MODIFY] `DatabaseManagerImpl.kt` (и `RedisManagerImpl.kt` если применимо)
- Добавление метода `close()`, который будет корректно выключать пул `HikariDataSource` и `RedisClient` во время `ApplicationStopping`.

---

### `feature/user` (Stateless Architecture / WebSockets)

Изменения для избавления от локального состояния при отправке WebSocket-сообщений.

#### [MODIFY] `KtorWebSocketManager.kt`
- Подписка на Redis Pub/Sub канал (например, `websocket_messages`).
- Вместо прямого вызова `sendMessageToSession(..., frame)` для пользователя или скоупа, менеджер будет сериализовывать кадр в Redis (через `RedisManager`).
- При получении сообщения из Redis канал, каждый инстанс будет проверять свои локальные `userIdToWebSocketSessions` и `userSessionIdToWebSocketSessions`, и отправлять сообщение, если клиент присоединен к этому серверу.

## Verification Plan

### Automated Tests
- Запуск существующих unit и интеграционных тестов через `gradlew test` (ожидается, что ничего не упадет).
- Написание тестов для `ManagementHealthRouter` (проверка кодов ответа 200 и 503).
- Обновление тестов `HealthCheckerManager` с учетом асинхронного метода.

### Manual Verification
1. Запуск приложения. Обращение к `/health/live` и `/health/ready` на management-порту. Отключение базы данных и проверка, что `/health/ready` переходит в `503`.
2. Остановка приложения по `SIGTERM` — проверка логов, что пулы соединений корректно и мягко закрываются без обрывов.
3. Проброс заголовка `X-Forwarded-For: 192.168.1.1` через curl — проверка, что приложение видит этот IP (например, в Audit-логах).