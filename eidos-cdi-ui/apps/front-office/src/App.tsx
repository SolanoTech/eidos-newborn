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
import { CustomerProvider } from "./CustomerContext";
import { Layout } from "./Layout";
import { WelcomePage } from "./pages/WelcomePage";
import { SearchPage } from "./pages/SearchPage";
import { MerchantPage } from "./pages/MerchantPage";
import { C360Page } from "./pages/C360Page";
import { NexusPage } from "./pages/NexusPage";
import { OffersPage } from "./pages/OffersPage";
import { CommsPage } from "./pages/CommsPage";
import { ChatPage } from "./pages/ChatPage";

export default function App() {
  return (
    <AuthProvider>
      <CustomerProvider>
        <Routes>
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<WelcomePage />} />
            <Route path="search" element={<SearchPage />} />
            <Route path="merchant/:merchantId" element={<MerchantPage />} />
            <Route path="c360" element={<C360Page />} />
            <Route path="nexus" element={<NexusPage />} />
            <Route path="offers" element={<OffersPage />} />
            <Route path="comms" element={<CommsPage />} />
            <Route path="chat" element={<ChatPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </CustomerProvider>
    </AuthProvider>
  );
}
