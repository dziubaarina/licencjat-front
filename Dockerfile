FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
ENV NODE_OPTIONS="--max-old-space-size=128"
RUN gradle jsBrowserDistribution --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx256m -XX:MaxMetaspaceSize=128m" -Dkotlin.daemon.jvmargs="-Xmx256m"

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