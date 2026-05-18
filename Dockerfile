FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle jsBrowserProductionWebpack --no-daemon -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=256m"

RUN mkdir -p /app/frontend-dist && \
    cp -r $(dirname $(find /app/build -name "main.bundle.js" | head -n 1))/* /app/frontend-dist/

FROM nginx:stable-alpine
COPY --from=build /app/frontend-dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]