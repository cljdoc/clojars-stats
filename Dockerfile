FROM clojure:tools-deps

WORKDIR /app
COPY deps.edn .
RUN clojure -Spath

COPY src/ ./src
COPY resources/ ./resources

CMD ["clojure", "-m", "cljdoc.clojars-stats", "/storage/clojars-stats.db"]
