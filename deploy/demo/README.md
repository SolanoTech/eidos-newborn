# Демонстрационный стенд

Урезанная сборка платформы для публичного показа интерфейса. Полная инструкция
— [Демонстрационный стенд](../../docs/operations/demo-stand.md); здесь только
состав каталога.

| Файл | Назначение |
|---|---|
| `../../docker-compose.demo.yml` | состав стенда: без Gravitee, OpenSearch одним узлом, наружу только Caddy |
| `Caddyfile` | TLS от Let's Encrypt, маршрутизация по трём именам |
| `setup-keycloak.sh` | публичные адреса в `redirectUris`, замена пароля из репозитория |
| `seed.py` | выдуманные карточки: без них консоли пустые |
| `docker-compose.seed.yml` | наложение на время засева: открывает два сервиса на 127.0.0.1 |
| `.env.demo.example` | секреты и имена; значений по умолчанию нет |

Коротко:

```bash
cp deploy/demo/.env.demo.example .env          # заполнить
docker compose -f docker-compose.demo.yml up -d --build
./deploy/demo/setup-keycloak.sh
ADMIN_TOKEN=$(grep '^ADMIN_TOKEN=' .env | cut -d= -f2-) python3 keycloak/setup_gravitee.py

# засев: сервисы на время открываются на петлевом интерфейсе
docker compose -f docker-compose.demo.yml -f deploy/demo/docker-compose.seed.yml up -d eidos-stage eidos-gateway
python3 -I deploy/demo/seed.py
docker compose -f docker-compose.demo.yml up -d eidos-stage eidos-gateway
```

Стенд рассчитан только на выдуманные данные: у внешнего API нет разграничения
по источникам. Подробности — в документации по ссылке выше.
