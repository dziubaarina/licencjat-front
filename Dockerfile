FROM nginx:stable-alpine
COPY build/dist/js/productionExecutable /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]