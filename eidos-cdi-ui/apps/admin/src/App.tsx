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

import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, ProtectedRoute } from "@eidos/auth";
import { Layout } from "./Layout";
import { WelcomePage } from "./pages/WelcomePage";
import { DashboardPage } from "./pages/DashboardPage";
import { SourcesPage } from "./pages/SourcesPage";
import { ConstructorPage } from "./pages/ConstructorPage";
import { SchemaPage } from "./pages/SchemaPage";
import { KafkaPage } from "./pages/KafkaPage";
import { ConflictsPage } from "./pages/ConflictsPage";
import { QualityPage } from "./pages/QualityPage";
import { SearchPage } from "./pages/SearchPage";
import { RecordCardPage } from "./pages/RecordCardPage";
import { LegalSearchPage } from "./pages/LegalSearchPage";
import { LegalCardPage } from "./pages/LegalCardPage";
import { ConsentPage } from "./pages/ConsentPage";
import { IntegrationsPage } from "./pages/IntegrationsPage";
import { AccessPage } from "./pages/AccessPage";
import { AuditPage } from "./pages/AuditPage";
import { PrivacyPage } from "./pages/PrivacyPage";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<WelcomePage />} />
          <Route path="dash" element={<DashboardPage />} />
          <Route path="sources" element={<SourcesPage />} />
          <Route path="constructor" element={<ConstructorPage />} />
          <Route path="schema" element={<SchemaPage />} />
          <Route path="kafka" element={<KafkaPage />} />
          <Route path="conflicts" element={<ConflictsPage />} />
          <Route path="quality" element={<QualityPage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="golden-records/:clientId" element={<RecordCardPage />} />
          <Route path="legal-search" element={<LegalSearchPage />} />
          <Route path="legal-records/:legalId" element={<LegalCardPage />} />
          <Route path="consent" element={<ConsentPage />} />
          <Route path="integrations" element={<IntegrationsPage />} />
          <Route path="access" element={<AccessPage />} />
          <Route path="audit" element={<AuditPage />} />
          <Route path="privacy" element={<PrivacyPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
