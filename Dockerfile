FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
ENV NODE_OPTIONS="--max-old-space-size=2048"
RUN gradle jsBrowserDistribution --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx1g -XX:MaxMetaspaceSize=256m" \
    -Dkotlin.daemon.jvmargs="-Xmx1g"

FROM nginx:alpine
COPY --from=build /app/build/dist/js/productionExecutable /usr/share/nginx/html
RUN echo 'server { listen 80; location / { root /usr/share/nginx/html; index index.html index.htm; try_files $uri $uri/ /index.html; } }' > /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]