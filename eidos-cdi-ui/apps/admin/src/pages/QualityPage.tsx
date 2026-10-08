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

import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { CardPanel, Chip, Icon, SegItem, ViewBar, toast } from "@eidos/ui-kit";
import { getDashboard, type CoreMetrics } from "../api/dashboard";

export function QualityPage() {
  const navigate = useNavigate();
  const [m, setM] = useState<CoreMetrics | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboard().then((d) => setM(d.metrics)).catch(() => setError("Не удалось загрузить метрики качества."));
  }, []);

  return (
    <section className="view-panel">
      <div className="wrap">
        <ViewBar
          title="Качество данных"
          sub="Мониторинг валидности и полноты Golden Records. Автоматические инциденты — в следующей фазе."
        />
        {error && <div className="errmsg" style={{ marginBottom: 16 }}>{error}</div>}
        <div className="grid g-2">
          <CardPanel icon={<Icon name="chart" size={16} />} title="Метрики по текущим данным">
            <SegItem label="Валидность ПИНФЛ" pct={m?.pinflValidPct ?? 0} tone="g" />
            <SegItem label={"Валидность телефона (^998\\d{9}$)"} pct={m?.phoneValidPct ?? 0} tone="g" />
            <SegItem label="Полнота отчества" pct={m?.middleNamePct ?? 0} />
            <SegItem label="Свежесть (обновлено < 30 дн.)" pct={m?.fresh30dPct ?? 0} />
          </CardPanel>
          <CardPanel
            icon={<Icon name="alert" size={16} />}
            title="Открытые инциденты"
            right={<Chip tone="info">демо</Chip>}
          >
            <div className="lrow">
              <div className="lic"><Icon name="alert" size={17} /></div>
              <div className="lmain">
                <div className="lnm">Всплеск отклонений валидацией: scoring</div>
                <div className="lsub">42 → 318 записей/час · поле birth_dt, формат даты</div>
              </div>
              <div className="lend">
                <Chip tone="bad">критично</Chip>
                <button className="btn btn-ghost btn-xs" onClick={() => toast("Демо: инцидент назначен")}>Назначить</button>
              </div>
            </div>
            <div className="lrow">
              <div className="lic"><Icon name="warnCircle" size={17} /></div>
              <div className="lmain">
                <div className="lnm">Рост доли пустых ПИНФЛ: partner-x</div>
                <div className="lsub">3.1% против нормы 0.4% · последние 6 часов</div>
              </div>
              <div className="lend">
                <Chip tone="warn">внимание</Chip>
                <button className="btn btn-ghost btn-xs" onClick={() => toast("Демо: инцидент назначен")}>Назначить</button>
              </div>
            </div>
            <div className="lrow">
              <div className="lic"><Icon name="checkCircle" size={17} /></div>
              <div className="lmain">
                <div className="lnm">Дрейф схемы: tieto добавил поле person.iin</div>
                <div className="lsub">Неизвестное поле игнорируется · предложить маппинг?</div>
              </div>
              <div className="lend">
                <Chip tone="info">инфо</Chip>
                <button className="btn btn-ghost btn-xs" onClick={() => { navigate("/constructor"); toast("Открыт конструктор контракта"); }}>
                  В конструктор
                </button>
              </div>
            </div>
          </CardPanel>
        </div>
      </div>
    </section>
  );
}
