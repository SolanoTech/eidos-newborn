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

import { useNavigate } from "react-router-dom";
import { WelcomeDrop } from "@eidos/ui-kit";

export function WelcomePage() {
  const navigate = useNavigate();
  return (
    <section className="view-panel" style={{ padding: 0 }}>
      <WelcomeDrop
        title="EIDOS"
        sub="Customer Data Platform"
        hint="Наведите на панель слева и выберите функцию. Начните с поиска клиента, чтобы открыть его профиль."
        cta={
          <button className="btn btn-primary" onClick={() => navigate("/search")}>
            Найти клиента
          </button>
        }
      />
    </section>
  );
}
