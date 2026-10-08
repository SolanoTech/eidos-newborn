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

import { EmptyState } from "@eidos/ui-kit";

// Каркас консоли маркетинга. Экраны появятся после переноса дизайна.
export default function App() {
  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
      <div style={{ width: "100%", maxWidth: 440 }}>
        <EmptyState
          icon="dashboard"
          title="Маркетинг — в разработке"
          hint="Общий каркас (дизайн-система, auth, API-клиент) подключён. Экраны добавим по макету."
        />
      </div>
    </div>
  );
}
