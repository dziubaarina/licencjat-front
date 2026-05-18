FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle jsBrowserProductionWebpack --no-daemon -Dorg.gradle.jvmargs="-Xmx3g -XX:MaxMetaspaceSize=512m"

FROM nginx:stable-alpine
COPY --from=build /app/build/distributions /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]