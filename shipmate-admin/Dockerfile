FROM node:24-alpine AS builder

WORKDIR /workspace
ARG VITE_SHIPMATE_API_BASE_URL=http://localhost:8080
ARG VITE_BASE_PATH=/
ENV VITE_SHIPMATE_API_BASE_URL=$VITE_SHIPMATE_API_BASE_URL
ENV VITE_BASE_PATH=$VITE_BASE_PATH
COPY shipmate-admin/package.json shipmate-admin/package-lock.json ./
RUN npm ci
COPY shipmate-admin/ ./
RUN npm run build

FROM nginx:1.29-alpine

COPY shipmate-admin/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/dist /usr/share/nginx/html
EXPOSE 80
