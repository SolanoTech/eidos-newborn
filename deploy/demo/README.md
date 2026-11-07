# Демонстрационный стенд

Урезанная сборка платформы для публичного показа интерфейса. Полная инструкция
— [Демонстрационный стенд](../../docs/operations/demo-stand.md); здесь только
состав каталога.

| Файл | Назначение |
|---|---|
| `../../docker-compose.demo.yml` | состав стенда: без Gravitee, OpenSearch одним узлом, наружу только Caddy |
| `Caddyfile` | TLS от Let's Encrypt, маршрутизация по трём именам, пароль на входе |
| `nginx-console.conf` | конфиг консолей без Gravitee: админские пути идут прямо в сервисы |
| `setup-keycloak.sh` | публичные адреса в `redirectUris`, замена пароля из репозитория |
| `seed.py` | выдуманные карточки: без них консоли пустые |
| `.env.demo.example` | секреты и имена; значений по умолчанию нет |
| `caddy.env.example` | хеш пароля отдельно: bcrypt содержит `$` |

Коротко:

```bash
cp deploy/demo/.env.demo.example .env          # заполнить
cp deploy/demo/caddy.env.example deploy/demo/caddy.env
docker compose -f docker-compose.demo.yml up -d --build
./deploy/demo/setup-keycloak.sh
python3 -I deploy/demo/seed.py
```

Стенд рассчитан только на выдуманные данные. Почему так и что именно заменяет
Gravitee — в документации по ссылке выше.
