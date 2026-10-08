# Резервное копирование

## Что копировать

| Данные | Где | Важность | Восстанавливается иначе? |
|---|---|---|---|
| Золотые записи, происхождение, архив, связки, очередь разбора, ключи `vault` | PostgreSQL `eidos_core` | Критично | Нет |
| Ключ шифрования ключей | OpenBao | Критично | Нет: без него ключи из `vault` не расшифровать |
| Реестр источников, контракты, статистика приёма | PostgreSQL `eidos_registry` | Критично | Частично — из описания источников как кода, см. [Настройка через API](automation.md) |
| Согласия | PostgreSQL `consents` | Критично | Нет |
| Пользователи и настройки входа | PostgreSQL `keycloak` | Высокая | Realm — из `realm-eidos.json`, пользователи — нет |
| Сырьё источников | MongoDB `eidos_stage` | Высокая | Нет. Нужно, чтобы восстановить исходные данные и состояние записи при создании |
| Журнал действий консоли | PostgreSQL `eidos_ui_backend` | Средняя | Нет |
| Настройки потребителей Kafka | PostgreSQL `gateway` | Низкая | Да — заново через консоль или API |
| Маршруты Gravitee | Каталог `gravitee/mongodb/data` | Низкая | Да — `setup_gravitee.py` |
| Секреты (`.env`, файлы переопределений) | Сервер | Критично | Нет |

Сообщения в топиках Kafka — транзитные данные. На стенде у брокера нет тома, и
сообщения теряются при пересоздании контейнера.

## Согласованность

База `eidos_core` и OpenBao связаны: ключи в схеме `vault` завёрнуты версиями
KEK из хранилища. Копируйте их вместе и восстанавливайте парой из одной точки
времени. Копия базы без соответствующего KEK бесполезна для слепого индекса и
хранилища токенов.

## Команды

### PostgreSQL

```bash
mkdir -p backup/$(date +%F)
for db in eidos_core eidos_registry consents gateway eidos_ui_backend keycloak; do
  docker compose exec -T postgres pg_dump -U postgres -Fc "$db" > "backup/$(date +%F)/$db.dump"
done
```

Восстановление одной базы:

```bash
docker compose exec -T postgres pg_restore -U postgres --clean --if-exists \
  -d eidos_core < backup/2026-10-04/eidos_core.dump
```

Останавливайте сервис, которому принадлежит база, на время восстановления.

### MongoDB

```bash
docker compose exec -T mongo mongodump --db eidos_stage --archive --gzip > backup/$(date +%F)/eidos_stage.archive.gz
docker compose exec -T mongo mongorestore --drop --archive --gzip < backup/2026-10-04/eidos_stage.archive.gz
```

### OpenBao

Режим разработки, в котором OpenBao работает на стенде, держит данные в памяти,
и копировать их нельзя. На сервере используйте постоянное хранилище и его
средства, например снимки Raft:

```bash
bao operator raft snapshot save eidos-openbao-$(date +%F).snap
```

См. [OpenBao](openbao.md#резервное-копирование).

## Регламент

- Копии баз и OpenBao — ежедневно и перед каждым обновлением.
- Храните копии отдельно от сервера платформы и шифруйте их: в них
  персональные данные.
- Проверяйте восстановление на отдельном стенде хотя бы раз в квартал:
  восстановите базы и OpenBao, запустите платформу и найдите несколько
  известных клиентов через поиск по ПИНФЛ — это проверяет и данные, и ключи.
