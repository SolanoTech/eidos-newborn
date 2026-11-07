# Монорепо консолей: собирается воркспейс, указанный build-arg APP
# (admin | front-office | marketing), раздаётся nginx.
#
# Адрес Keycloak, realm и клиент попадают в бандл на этапе сборки: это
# переменные Vite, во время выполнения их не подменить. Значения по умолчанию
# рассчитаны на стенд из docker-compose.yml; для установки на сервере передайте
# свои — иначе вход с другой машины работать не будет.
FROM node:22-alpine AS build
ARG APP=admin
ARG VITE_KC_URL=http://localhost:8180
ARG VITE_KC_REALM=eidos
ARG VITE_KC_CLIENT_ID=eidos-console
ENV VITE_KC_URL=${VITE_KC_URL} \
    VITE_KC_REALM=${VITE_KC_REALM} \
    VITE_KC_CLIENT_ID=${VITE_KC_CLIENT_ID}
WORKDIR /app
COPY . .
RUN npm ci
RUN npm run build -w @eidos/${APP}

FROM nginx:1.27-alpine
ARG APP=admin
COPY apps/${APP}/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/apps/${APP}/dist /usr/share/nginx/html
EXPOSE 80
