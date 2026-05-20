FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle jsBrowserDistribution --no-daemon -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=256m"

RUN mkdir -p /app/frontend-dist && \
    # search the whole build tree for index.html (Kotlin/JS may place it under processedResources/js/main)
    DIST_DIR=$(find /app/build -name "index.html" -exec dirname {} \; | head -n 1) && \
    if [ -n "$DIST_DIR" ]; then \
        cp -r "$DIST_DIR"/* /app/frontend-dist/; \
    else \
        echo "Error: index.html not found in build output!"; exit 1; \
    fi

FROM nginx:stable-alpine
COPY --from=build /app/frontend-dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]