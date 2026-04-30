FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew browserDistribution --no-daemon

FROM nginx:stable-alpine

COPY --from=build /app/build/dist/js/productionExecutable /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]