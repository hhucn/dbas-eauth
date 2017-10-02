FROM clojure:alpine

COPY . /code
WORKDIR /code

EXPOSE 7878
CMD ["lein", "repl", ":headless", ":host", "0.0.0.0", ":port", "7878"]
