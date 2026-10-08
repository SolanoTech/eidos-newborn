# Монорепо консолей: собирается воркспейс, указанный build-arg APP
# (admin | front-office | marketing), раздаётся nginx.
FROM node:22-alpine AS build
ARG APP=admin
WORKDIR /app
COPY . .
RUN npm ci
RUN npm run build -w @eidos/${APP}

FROM nginx:1.27-alpine
ARG APP=admin
COPY apps/${APP}/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/apps/${APP}/dist /usr/share/nginx/html
EXPOSE 80
