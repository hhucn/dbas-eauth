FROM clojure:alpine

COPY . /app
WORKDIR /app

EXPOSE 8080 7887

CMD ["lein", "repl", ":headless", ":host", "0.0.0.0", ":port", "7887"]
