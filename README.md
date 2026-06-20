# Reservvo

API REST para agendamento de horários entre prestadores de serviço e clientes. Provider cadastra negócio, recursos e regras de disponibilidade. Cliente reserva slots dentro dessas regras. O sistema detecta conflitos e dispara emails transacionais.

## Stack

- Java 25 + Spring Boot 4.0
- PostgreSQL 17 (prod) / H2 (testes)
- Redis 7 (cache + streams)
- AWS SES (envio de email)
- Spring Security + JJWT
- Maven, Lombok
- JUnit 5 + Mockito + MockMvc

## Estrutura

```
src/main/java/com/odgiedev/reservvo/
├── config/         Beans Spring (Redis, Streams, CORS, SES)
├── controller/     Endpoints REST
├── dto/
│   ├── request/    Records de input com validação
│   └── response/   Records de output
├── entity/         JPA entities
├── enums/          Status, Role, NotificationType
├── exception/      BusinessException + handler global
├── repository/     Spring Data JPA
├── scheduler/      Cron jobs
├── security/       JWT, filtro, config
├── service/        Lógica de negócio
└── util/           StringUtils
```

Camadas: `Controller → Service → Repository → Entity`. Notificações saem do `Service` de forma assíncrona via Redis Stream.

## Setup

Requer Docker e Docker Compose.

```bash
#setar PLACEHOLDERs application.properties

docker compose up --build
```

App sobe em `http://localhost:8080`. Swagger em `/swagger-ui`.

## Variáveis de ambiente

| Variável | Default | Obrigatória |
|---|---|---|
| `SPRING_PROFILE` | `dev` | não |
| `DB_USERNAME` / `DB_PASSWORD` | `reservvo` / `reservvo` | sim em prod |
| `JWT_SECRET` | — | **sim** (mín. 32 caracteres) |
| `REDIS_PASSWORD` | vazio | não |
| `AWS_REGION` | `sa-east-1` | sim para envio real de email |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | — | sim para envio real de email |

## API

Base: `/api`. Auth via header `Authorization: Bearer <jwt>`.

| Método | Rota | Auth |
|---|---|---|
| POST | `/auth/register` | — |
| POST | `/auth/login` | — |
| GET | `/provider/slug/{slug}` | — |
| POST | `/provider` | sim |
| PUT | `/provider` | sim |
| POST | `/resources` | sim |
| PUT | `/resources/{id}` | sim |
| PUT | `/resources/{id}/availability-rules` | sim |
| GET | `/resources/provider/{providerId}` | — |
| POST | `/reservations` | sim |
| GET | `/reservations/client?status=&page=&size=` | sim |
| GET | `/reservations/provider?status=&page=&size=` | sim |
| GET | `/reservations/stats` | sim |
| GET | `/reservations/slots?resourceId=&date=` | sim |
| PATCH | `/reservations/{id}/cancel/client` | sim |
| PATCH | `/reservations/{id}/cancel/provider` | sim |

Documentação completa no Swagger.

## Domínio

```
User ──(1:1)── Provider ──(1:N)── Resource ──(1:N)── AvailabilityRule
                                      │
User (como cliente) ──(1:N)── Reservation ──┘
```

- **User** — `role`: `CLIENT`, `PROVIDER` ou `BOTH`
- **Provider** — o negócio; slug único, telefone, categoria
- **Resource** — o que pode ser reservado (cadeira, sala, mesa)
- **AvailabilityRule** — janela horária por dia da semana (0=domingo, 6=sábado)
- **Reservation** — status: `CONFIRMED`, `COMPLETED`, `CANCELLED_BY_CLIENT`, `CANCELLED_BY_PROVIDER`

## Testes

```bash
./mvnw test
```

H2 in-memory. Redis desabilitado via `spring.cache.type=none` e `@Profile("!test")` no `RedisConfig`. AWS SES e RedisTemplate são `@MockitoBean` no `BaseIntegrationTest`.

## Decisões técnicas

Contexto e trade-offs das principais escolhas (Redis Streams + DLQ, prevenção de N+1, cache de slots): **[devdiegofernandes.com/projects/reservvo](https://devdiegofernandes.com/projects/reservvo)**
