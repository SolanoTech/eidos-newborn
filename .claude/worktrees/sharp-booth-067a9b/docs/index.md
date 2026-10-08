---
title: Документация Eidos CDI
hide:
  - navigation
  - toc
---

# Eidos CDI

**Eidos CDI** (Customer Data Integration) — модуль клиентской платформы данных
Eidos, который собирает сведения о клиентах из разрозненных систем-источников и
сводит их в единый профиль — **Золотую запись**. Модуль принимает данные по REST
и через Kafka, приводит их к общему виду по настраиваемому дата-контракту,
находит совпадения с уже известными клиентами, сливает поля по уровню доверия
источника и хранит происхождение каждого значения. Спорные случаи попадают в
очередь на ручной разбор.

Платформа работает с двумя видами клиентов: **физическими лицами**
(сопоставление по ПИНФЛ и паспорту) и **юридическими лицами — мерчантами**
(сопоставление по ИНН, нечёткий поиск по названию).

## С чего начать

<div class="grid cards" markdown>

-   :material-lightbulb-on-outline:{ .lg .middle } **Оцениваю платформу**

    ---

    Что делает модуль, из чего состоит и каковы ограничения текущей версии.

    [:octicons-arrow-right-24: Что такое Eidos CDI](intro/what-is-eidos-cdi.md) ·
    [Архитектура](intro/architecture.md)

-   :material-rocket-launch-outline:{ .lg .middle } **Хочу попробовать**

    ---

    Поднимите стенд на своей машине и получите первую Золотую запись.

    [:octicons-arrow-right-24: Быстрый старт](quickstart/stand.md)

-   :material-database-import-outline:{ .lg .middle } **Подключаю источник**

    ---

    Токен, дата-контракт, передача по REST или Kafka, разбор ошибок.

    [:octicons-arrow-right-24: Подключение источников](sources/onboarding.md)

-   :material-database-search-outline:{ .lg .middle } **Забираю данные**

    ---

    Поиск Золотой записи, внешние идентификаторы, проверка согласий.

    [:octicons-arrow-right-24: Получение данных](consumers/search.md)

-   :material-monitor-dashboard:{ .lg .middle } **Работаю в консоли**

    ---

    Реестр источников, конструктор контракта, очередь конфликтов, карточка
    «Клиент 360».

    [:octicons-arrow-right-24: Консоли](console/login.md)

-   :material-server-network:{ .lg .middle } **Разворачиваю платформу**

    ---

    Установка, Keycloak, Gravitee, OpenBao, резервное копирование и защита.

    [:octicons-arrow-right-24: Эксплуатация](operations/requirements.md)

-   :material-book-open-variant:{ .lg .middle } **Ищу точный ответ**

    ---

    API всех сервисов, поля Золотой записи, параметры конфигурации, коды ошибок.

    [:octicons-arrow-right-24: Справочник](reference/api/external.md)

-   :material-source-pull:{ .lg .middle } **Дорабатываю код**

    ---

    Устройство репозиториев, сборка, расширение модели данных, правила вклада.

    [:octicons-arrow-right-24: Разработка](development/repositories.md)

</div>

## О документации

Документация описывает поведение платформы по состоянию основной ветки (`main`)
всех репозиториев модуля. Пронумерованных выпусков пока нет — см.
[Совместимость версий](reference/compatibility.md). Известные ограничения текущей
версии собраны на странице [Статус и ограничения](intro/status.md): прочтите её
до того, как планировать промышленное внедрение.

Исходный код и документация распространяются под лицензией
[Apache License 2.0](legal.md). Нашли неточность — откройте issue или pull
request в репозиторий
[eidos-infrastructure](https://github.com/SolanoTech/eidos-infrastructure):
документация лежит в каталоге `docs/`.
