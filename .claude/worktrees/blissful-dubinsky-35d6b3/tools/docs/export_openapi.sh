#!/usr/bin/env bash
# Copyright 2026 LLC SOLANOTECH
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Выгружает OpenAPI-спецификации сервисов в docs/reference/openapi/.
#
# Спецификацию пишет тест OpenApiSpecTest каждого сервиса: он поднимает
# веб-слой без баз и брокера и сохраняет то, что сервис отдаёт на /v3/api-docs.
# Стенд не нужен. Тест заодно проверяет описания — сервис с неописанной
# операцией не выгрузится.
#
# Запуск из корня репозитория, сервисы склонированы рядом (см. README):
#
#     tools/docs/export_openapi.sh                  # все сервисы
#     tools/docs/export_openapi.sh eidos-gateway    # выборочно
#
# Нужен JDK 26 (JAVA_HOME). eidos-core и eidos-stage собираются с
# shared-library из локального репозитория Maven: после её изменения
# выполните mvn -f shared-library/pom.xml install.
#
# Результат — <сервис>.yaml со спецификацией целиком; у сервисов с внешним API
# (eidos-core, eidos-gateway) — ещё <сервис>-external.yaml и
# <сервис>-internal.yaml по группам.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/docs/reference/openapi"
SERVICES=(eidos-core eidos-stage eidos-gateway eidos-consents eidos-cdi-ui-backend)

if [ "$#" -gt 0 ]; then
    SERVICES=("$@")
fi

mkdir -p "$OUT"
for service in "${SERVICES[@]}"; do
    dir="$ROOT/$service"
    if [ ! -f "$dir/pom.xml" ]; then
        echo "Нет модуля $service: склонируйте его в корень репозитория (см. README)" >&2
        exit 1
    fi
    # У eidos-stage нет Maven Wrapper — нужен установленный Maven.
    if [ -x "$dir/mvnw" ]; then
        mvn="./mvnw"
    else
        mvn="mvn"
    fi
    echo "→ $service"
    # Вывод тестов — в target/surefire-reports; при провале Maven покажет причину.
    (cd "$dir" && "$mvn" -q -B test -Dtest=OpenApiSpecTest \
        -Dmaven.test.redirectTestOutputToFile=true \
        -Deidos.openapi.export-dir="$OUT")
done

echo "Спецификации — в ${OUT#"$ROOT"/}"
