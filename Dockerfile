FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle jsBrowserProductionWebpack --no-daemon -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=256m"

RUN mkdir -p /app/frontend-dist && \
    DIST_DIR=$(find /app/build -name "main.bundle.js" -exec dirname {} \; | head -n 1) && \
    if [ -n "$DIST_DIR" ]; then \
        cp -r "$DIST_DIR"/* /app/frontend-dist/; \
    else \
        echo "Error: main.bundle.js not found!"; exit 1; \
    fi

FROM nginx:stable-alpine
COPY --from=build /app/frontend-dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]