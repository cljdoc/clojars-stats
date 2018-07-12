FROM clojure:tools-deps

WORKDIR /app
COPY src/ ./src
COPY resources/ ./resources
COPY deps.edn .

RUN clojure -Spath

CMD ["clojure", "-m", "cljdoc.clojars-stats", "/storage/clojars-stats.db"]
