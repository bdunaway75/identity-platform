# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build

WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY frontend ./

ARG VITE_AUTH_SERVER_ORIGIN
ARG VITE_FRONTEND_ORIGIN
ARG VITE_OIDC_CLIENT_ID
ARG VITE_BETA_MODE

ENV VITE_AUTH_SERVER_ORIGIN=${VITE_AUTH_SERVER_ORIGIN}
ENV VITE_FRONTEND_ORIGIN=${VITE_FRONTEND_ORIGIN}
ENV VITE_OIDC_CLIENT_ID=${VITE_OIDC_CLIENT_ID}
ENV VITE_BETA_MODE=${VITE_BETA_MODE}

RUN --mount=type=cache,target=/root/.npm npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/frontend/dist /usr/share/nginx/html

EXPOSE 80
