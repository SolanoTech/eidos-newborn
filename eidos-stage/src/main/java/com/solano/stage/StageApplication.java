/*
 * Copyright 2026 LLC SOLANOTECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solano.stage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа eidos-stage. Spring Boot — единый фреймворк со всеми сервисами
 * SCV. Сервис принимает клиентские данные по REST
 * ({@code POST /api/v1/client-data}), сохраняет сырьё в MongoDB, трансформирует
 * их в общий контракт и отправляет в eidos-core.
 */
@SpringBootApplication
public class StageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StageApplication.class, args);
    }
}
