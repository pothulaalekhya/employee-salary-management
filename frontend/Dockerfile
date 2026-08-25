# ==============================================================================
# Stage 1: Build Angular Application using Node 20
# ==============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies first
COPY package*.json ./
RUN npm install

# Copy application source code
COPY . .

# Build production bundle
RUN npm run build -- --configuration production

# ==============================================================================
# Stage 2: Serve static files with lightweight Nginx web server
# ==============================================================================
FROM nginx:1.27-alpine

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled Angular distribution files from builder stage
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
