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

package com.solano.eidoscore.web;

import com.solano.eidoscore.entity.TentativeReason;
import com.solano.eidoscore.service.legal.LegalEntityTentativeService;
import com.solano.eidoscore.web.dto.TentativeLegalItemView;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Очередь конфликтов юрлиц — зеркало {@link TentativeController}: список,
 * счётчики и разрешение оператором.
 */
@RestController
@RequestMapping("/api/v1/internal/tentative-legal")
public class TentativeLegalController {

    public record ResolveRequest(LegalEntityTentativeService.ResolveAction action) {
    }

    private final LegalEntityTentativeService tentativeService;

    public TentativeLegalController(LegalEntityTentativeService tentativeService) {
        this.tentativeService = tentativeService;
    }

    @GetMapping
    public Page<TentativeLegalItemView> list(
            @RequestParam(required = false) TentativeReason reason,
            @RequestParam(defaultValue = "1") int page
    ) {
        return tentativeService.list(reason, page);
    }

    @GetMapping("/count")
    public Map<String, Long> counts() {
        return tentativeService.counts();
    }

    @PostMapping("/{id}/resolve")
    public Map<String, String> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
        tentativeService.resolve(id, request.action());
        return Map.of("status", "RESOLVED");
    }
}
