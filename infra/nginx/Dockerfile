FROM node:22-alpine AS frontend-build

WORKDIR /app

RUN corepack enable

COPY frontendv2/package.json frontendv2/pnpm-lock.yaml frontendv2/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY frontendv2/ ./

RUN pnpm build

FROM nginx:alpine

COPY --from=frontend-build /app/dist /usr/share/nginx/html
COPY infra/nginx/nginx.conf /etc/nginx/conf.d/default.conf
