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
package com.solano.eidoscore.crypto;

import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.Mac;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.TinkProtoKeysetFormat;
import com.google.crypto.tink.mac.MacConfig;
import com.google.crypto.tink.mac.HmacParameters;
import com.solano.eidoscore.entity.vault.PdBlindIndexKey;
import com.solano.eidoscore.repository.vault.PdBlindIndexKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Слепой индекс: необратимая метка, по которой можно искать равенство, не имея
 * самого значения.
 *
 * <p>Считается для всех записей всегда — и для тех, чьи данные ещё открыты.
 * Иначе поиск зависел бы от состояния клиента, а записи «то находились, то
 * нет».</p>
 *
 * <p><b>Значения берутся как есть, без приведения регистра и обрезки.</b>
 * Действующий матчинг сравнивает поля на точное равенство, и индекс обязан
 * воспроизводить ту же семантику: любая нормализация здесь изменила бы то,
 * какие записи считаются одним человеком.</p>
 *
 * <p>Ключ читается из хранилища один раз при первом обращении и держится в
 * памяти: считать HMAC обращением к хранилищу на каждую запись — это сетевой
 * вызов на каждый приём карточки, чего приёмный тракт не выдержит. Цена —
 * ключ живёт в памяти процесса.</p>
 */
@Component
public class BlindIndex {

    private static final Logger log = LoggerFactory.getLogger(BlindIndex.class);

    /** Разделитель полей: непечатный символ, в данных не встречается. */
    private static final char SEP = '\u001f';

    static {
        try {
            MacConfig.register();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot initialise Tink MAC", e);
        }
    }

    private final PdBlindIndexKeyRepository keyRepository;
    private final KeyWrapper keyWrapper;
    private final Clock clock;

    private volatile Mac mac;
    private volatile Integer keyVersion;

    public BlindIndex(PdBlindIndexKeyRepository keyRepository, KeyWrapper keyWrapper, Clock clock) {
        this.keyRepository = keyRepository;
        this.keyWrapper = keyWrapper;
        this.clock = clock;
    }

    /** Версия действующего ключа — пишется рядом с индексом. */
    public int currentKeyVersion() {
        ensureKey();
        return keyVersion;
    }

    /** Основной ключ матчинга: фамилия + имя + дата рождения + ПИНФЛ. */
    public String identity(String lastName, String firstName, LocalDate birthDate, String pinfl) {
        return compute("id", lastName, firstName, birthDate, pinfl);
    }

    /** Откат матчинга: фамилия + имя + дата рождения + паспорт. */
    public String identityByDocument(String lastName, String firstName, LocalDate birthDate, String passport) {
        return compute("doc", lastName, firstName, birthDate, passport);
    }

    /** Одиночный ПИНФЛ — под точный поиск по номеру. */
    public String pinfl(String pinfl) {
        return compute("pinfl", pinfl);
    }

    /**
     * Считает индекс одной области. Первый аргумент — имя области: без него
     * разные комбинации совпали бы, случись одинаковые значения (например,
     * паспорт одного клиента и ПИНФЛ другого), и поиск начал бы путать их.
     *
     * <p>{@code null} в любой части означает, что индекс посчитать не из чего.</p>
     */
    private String compute(String domain, Object... parts) {
        StringBuilder sb = new StringBuilder(domain);
        for (Object part : parts) {
            if (part == null || (part instanceof String s && s.isEmpty())) {
                return null;
            }
            sb.append(SEP).append(part);
        }
        ensureKey();
        try {
            byte[] tag = mac.computeMac(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(tag);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot compute blind index", e);
        }
    }

    private void ensureKey() {
        if (mac != null) {
            return;
        }
        synchronized (this) {
            if (mac == null) {
                load();
            }
        }
    }

    private void load() {
        PdBlindIndexKey stored = keyRepository.findFirstByOrderByKeyVersionDesc()
                .orElseGet(this::createFirstKey);
        try {
            byte[] raw = keyWrapper.unwrap(stored.getWrappedKey());
            KeysetHandle handle = TinkProtoKeysetFormat.parseKeyset(raw, InsecureSecretKeyAccess.get());
            this.mac = handle.getPrimitive(RegistryConfiguration.get(), Mac.class);
            this.keyVersion = stored.getKeyVersion();
            log.info("Blind index key version {} loaded", keyVersion);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot load blind index key", e);
        }
    }

    private PdBlindIndexKey createFirstKey() {
        try {
            // NO_PREFIX: Tink иначе добавляет к тегу 5 байт идентификатора
            // ключа, и длина перестаёт быть постоянной. Версию ключа мы и так
            // храним своей колонкой, а фиксированные 32 байта дают ровно 64
            // шестнадцатеричных символа — как объявлено в схеме.
            HmacParameters params = HmacParameters.builder()
                    .setKeySizeBytes(32)
                    .setTagSizeBytes(32)
                    .setHashType(HmacParameters.HashType.SHA256)
                    .setVariant(HmacParameters.Variant.NO_PREFIX)
                    .build();
            KeysetHandle handle = KeysetHandle.generateNew(params);
            byte[] raw = TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get());
            PdBlindIndexKey key = PdBlindIndexKey.builder()
                    .keyVersion(1)
                    .wrappedKey(keyWrapper.wrap(raw))
                    .kekName(keyWrapper.kekName())
                    .createdAt(LocalDateTime.now(clock))
                    .build();
            log.info("No blind index key found, generating the first one");
            return keyRepository.save(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot generate blind index key", e);
        }
    }
}
