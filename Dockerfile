FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
# Budujemy bez sztucznych restrykcji pamięciowych, GitHub ma na to zasoby
ENV NODE_OPTIONS="--max-old-space-size=4096"
RUN gradle jsBrowserDistribution --no-daemon -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m"

FROM nginx:alpine
# Standardowy wyjściowy folder dla zadania jsBrowserDistribution w nowoczesnym Kotlin/JS
COPY --from=build /app/build/dist/js/productionExecutable /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]