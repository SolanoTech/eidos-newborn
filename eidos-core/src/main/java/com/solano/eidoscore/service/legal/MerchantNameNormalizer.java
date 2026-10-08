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

package com.solano.eidoscore.service.legal;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Нормализация названия мерчанта для неточного поиска.
 *
 * <p>Задача: «ООО "Оазис Маркет"», «OOO Oazis Market» и «OAZIS MARKET MCHJ»
 * должны сводиться к одной строке. Поэтому нормализация делает три вещи:</p>
 * <ol>
 *   <li>приводит к нижнему регистру и убирает кавычки/пунктуацию;</li>
 *   <li>транслитерирует кириллицу в латиницу (узбекский вариант) — источники
 *       присылают названия и так, и так;</li>
 *   <li>выбрасывает организационно-правовые формы, которые не несут смысла
 *       при поиске по бренду.</li>
 * </ol>
 *
 * <p>Результат кладётся в техническую колонку {@code name_normalized}, по
 * которой строится триграммный индекс.</p>
 */
public final class MerchantNameNormalizer {

    /** Организационно-правовые формы (уже после транслитерации в латиницу). */
    private static final Set<String> ORG_FORMS = Set.of(
            "ooo", "mchj", "mas'uliyati", "cheklangan", "jamiyat", "jamiyati",
            "ao", "aj", "aksiyadorlik", "yopiq", "ochiq", "zao", "oao", "pao",
            "xk", "hk", "xususiy", "korxona", "korxonasi", "chp", "yatt", "ytt",
            "dp", "sp", "qk", "llc", "ltd", "jsc", "inc", "co", "corp",
            "ip", "pe", "gup", "mup", "fh", "firma", "kompaniya", "company"
    );

    private MerchantNameNormalizer() {
    }

    /**
     * Нормализует несколько вариантов названия в одну строку поиска
     * (полное имя + бренд), отбрасывая пустые и повторяющиеся слова.
     */
    public static String normalizeAll(String... names) {
        Set<String> words = new LinkedHashSet<>();
        for (String name : names) {
            String normalized = normalize(name);
            if (normalized != null && !normalized.isBlank()) {
                for (String word : normalized.split(" ")) {
                    words.add(word);
                }
            }
        }
        return words.isEmpty() ? null : String.join(" ", words);
    }

    /** Нормализует одно название; {@code null} для пустого входа. */
    public static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String latin = transliterate(name.toLowerCase(Locale.ROOT));

        StringBuilder cleaned = new StringBuilder(latin.length());
        for (char c : latin.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                cleaned.append(c);
            } else if (c == '\'' || c == '`') {
                // апостроф значим в узбекской латинице (o', g') — оставляем
                cleaned.append('\'');
            } else {
                cleaned.append(' ');
            }
        }

        StringBuilder result = new StringBuilder();
        for (String word : cleaned.toString().split("\\s+")) {
            if (word.isBlank() || ORG_FORMS.contains(word)) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(word);
        }
        String out = result.toString().trim();
        return out.isEmpty() ? null : out;
    }

    /** Кириллица → узбекская латиница; латиница проходит без изменений. */
    private static String transliterate(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'а' -> sb.append('a');
                case 'б' -> sb.append('b');
                case 'в' -> sb.append('v');
                case 'г' -> sb.append('g');
                case 'ғ' -> sb.append("g'");
                case 'д' -> sb.append('d');
                case 'е' -> sb.append('e');
                case 'ё' -> sb.append("yo");
                case 'ж' -> sb.append('j');
                case 'з' -> sb.append('z');
                case 'и' -> sb.append('i');
                case 'й' -> sb.append('y');
                case 'к' -> sb.append('k');
                case 'қ' -> sb.append('q');
                case 'л' -> sb.append('l');
                case 'м' -> sb.append('m');
                case 'н' -> sb.append('n');
                case 'о' -> sb.append('o');
                case 'ў' -> sb.append("o'");
                case 'п' -> sb.append('p');
                case 'р' -> sb.append('r');
                case 'с' -> sb.append('s');
                case 'т' -> sb.append('t');
                case 'у' -> sb.append('u');
                case 'ф' -> sb.append('f');
                case 'х' -> sb.append('x');
                case 'ҳ' -> sb.append('h');
                case 'ц' -> sb.append("ts");
                case 'ч' -> sb.append("ch");
                case 'ш' -> sb.append("sh");
                case 'щ' -> sb.append("sh");
                case 'ъ', 'ь' -> { /* твёрдый и мягкий знаки отбрасываются */ }
                case 'ы' -> sb.append('i');
                case 'э' -> sb.append('e');
                case 'ю' -> sb.append("yu");
                case 'я' -> sb.append("ya");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
