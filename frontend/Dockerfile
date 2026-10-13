FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# `vite preview` serves the static build and proxies /api to the backend (see vite.config.ts).
# For real production hosting, swap this stage for nginx.
FROM node:22-alpine
WORKDIR /app
COPY --from=build /app ./
ENV API_PROXY_TARGET=http://backend:8080
EXPOSE 5173
CMD ["npx", "vite", "preview"]
