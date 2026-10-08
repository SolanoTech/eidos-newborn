# OpenBao

OpenBao хранит **ключ шифрования ключей** (KEK) платформы. Ядро не получает
KEK: оно передаёт хранилищу ключи данных и получает их зашифрованными или
расшифрованными через движок `transit`. Как устроен криптоконтур —
[Защита персональных данных](../concepts/data-protection.md).

## Что ядро делает с OpenBao

| Операция | Путь OpenBao | Когда |
|---|---|---|
| Включить движок transit | `POST /v1/sys/mounts/transit` | При старте ядра, если `EIDOS_CRYPTO_ENABLED=true` |
| Создать KEK (`aes256-gcm96`) | `POST /v1/transit/keys/<KEK>` | При старте ядра, если `EIDOS_CRYPTO_ENABLED=true` |
| Завернуть ключ | `POST /v1/transit/encrypt/<KEK>` | При создании ключа слепого индекса и ключей данных |
| Развернуть ключ | `POST /v1/transit/decrypt/<KEK>` | При первом обращении к слепому индексу после старта ядра |

Повторное создание движка и ключа безопасно: если они существуют, ядро
продолжает работу. Если OpenBao недоступен при старте, ядро запускается и
пишет ошибку в журнал, но сопоставление и поиск по ПИНФЛ не заработают, пока
хранилище не станет доступно.

Настройки ядра:

| Переменная | Назначение |
|---|---|
| `EIDOS_CRYPTO_OPENBAO_BASE_URL` | Адрес OpenBao |
| `EIDOS_CRYPTO_OPENBAO_TOKEN` | Токен доступа (в compose — из `OPENBAO_TOKEN`) |
| `EIDOS_CRYPTO_OPENBAO_KEK_NAME` | Имя KEK в transit (в compose — из `OPENBAO_KEK_NAME`, по умолчанию `eidos-pd-kek`) |
| `EIDOS_CRYPTO_ENABLED` | Создавать ли движок и KEK при старте. На работу слепого индекса не влияет: OpenBao нужен всегда |

## Стенд: режим разработки

В `docker-compose.yml` OpenBao запущен командой `server -dev`: данные в
памяти, хранилище распечатано автоматически, корневой токен равен
`OPENBAO_TOKEN`. Это удобно, но опасно:

!!! danger "Перезапуск OpenBao на стенде ломает сопоставление"
    После перезапуска контейнера OpenBao пуст. Ядро создаст новый KEK с тем же
    именем, но ключ слепого индекса в базе завёрнут старым KEK и больше не
    разворачивается. Приём карточек физлиц и поиск по ПИНФЛ завершаются
    ошибками `Cannot load blind index key` или `Key store operation failed`.

Как восстановить стенд, если данные не жалко:

```bash
docker compose down -v && docker compose up -d
```

Как восстановить стенд с сохранением Золотых записей: выпустить новый ключ
слепого индекса и пересчитать индекс.

```bash
docker compose exec -T postgres psql -U postgres -d eidos_core -c \
  "DELETE FROM vault.pd_blind_index_key; UPDATE golden_record SET pd_bi_key_version = NULL;"
docker compose restart eidos-core
```

После перезапуска ядро создаст в OpenBao новый KEK, выпустит новый ключ
слепого индекса и пересчитает индекс всех записей. Проверьте журнал:

```bash
docker compose logs eidos-core --since 2m | grep -i "blind index"
```

Ожидаемая строка — `Blind index backfill finished, N records processed`. Если
вместо неё `Blind index backfill failed`, перезапустите ядро ещё раз: при
первом старте пересчёт мог начаться раньше, чем был создан KEK.

Если хранилище токенов уже использовалось (таблицы `vault.pd_dek`,
`vault.pd_token`), зашифрованные в нём значения восстановить нельзя.

Не перезапускайте контейнер `openbao` отдельно от остального стенда без
необходимости.

## Сервер: постоянное хранилище

На сервере OpenBao должен переживать перезапуски. Требования:

- **постоянное хранилище** — встроенный Raft или другой поддерживаемый
  backend;
- **распечатывание** — ключами Шамира или автоматически через внешний KMS/HSM;
- **TLS** между ядром и OpenBao;
- **отдельный токен** для ядра с минимальной политикой вместо корневого.

Подготовьте движок и ключ один раз при установке:

```bash
bao secrets enable transit
bao write -f transit/keys/eidos-pd-kek type=aes256-gcm96
```

Политика для ядра — только шифрование и расшифровка этим ключом:

```hcl
path "transit/encrypt/eidos-pd-kek" {
  capabilities = ["update"]
}

path "transit/decrypt/eidos-pd-kek" {
  capabilities = ["update"]
}
```

```bash
bao policy write eidos-core eidos-core.hcl
bao token create -policy=eidos-core -period=768h
```

Передайте ядру полученный токен и задайте `EIDOS_CRYPTO_ENABLED=false`: движок
и ключ уже созданы, а токену с такой политикой их создание недоступно. С
периодическим токеном (`-period`) настройте его продление — например, через
агент OpenBao — или используйте другой метод аутентификации по правилам вашей
инфраструктуры.

## Ротация KEK

Движок transit хранит версии ключа:

```bash
bao write -f transit/keys/eidos-pd-kek/rotate
```

После ротации новые ключи заворачиваются новой версией KEK, а ранее
завёрнутые по-прежнему расшифровываются старыми версиями. Не поднимайте
`min_decryption_version` ключа: перезаворачивания сохранённых ключей в
платформе нет, и ключи, завёрнутые старыми версиями, перестанут
расшифровываться.

## Резервное копирование

Потеря KEK равносильна потере ключа слепого индекса и всех значений в
хранилище токенов. Делайте резервные копии OpenBao вместе с копиями базы
`eidos_core`: ключи в базе должны соответствовать версиям KEK в хранилище. Для
Raft:

```bash
bao operator raft snapshot save eidos-openbao.snap
```

См. [Резервное копирование](backup.md).
