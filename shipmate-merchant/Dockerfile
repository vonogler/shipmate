FROM node:24-alpine AS builder

WORKDIR /workspace
ARG VITE_SHIPMATE_API_BASE_URL=http://localhost:8080
ARG VITE_BASE_PATH=/
ENV VITE_SHIPMATE_API_BASE_URL=$VITE_SHIPMATE_API_BASE_URL
ENV VITE_BASE_PATH=$VITE_BASE_PATH
COPY shipmate-merchant/package.json shipmate-merchant/package-lock.json ./
RUN npm ci
COPY shipmate-merchant/ ./
RUN npm run build

FROM nginx:1.29-alpine

COPY shipmate-merchant/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/dist /usr/share/nginx/html
EXPOSE 80
