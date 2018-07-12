FROM clojure:tools-deps

WORKDIR /app
COPY deps.edn .
RUN clojure -Spath

COPY src/ ./src
COPY resources/ ./resources

EXPOSE 3000

CMD ["clojure", "-m", "cljdoc.clojars-stats", "/storage/clojars-stats.db"]
