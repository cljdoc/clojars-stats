# clojars-stats service

An attempt at making the data in https://clojars.org/stats/ a bit more accessible.

#### `/artifact-monthly`

E.g. https://clojars-stats.cljdoc.xyz/artifact-monthly?group_id=reagent&artifact_id=reagent

## Contributing

More to come as the need arises while working on https://cljdoc.xyz.

Run locally using `clj`
```
clj -m cljdoc.clojars-stats clojars-stats.db
```
or use the almighty REPL.

If you are interested in adding useful queries, feel invited to do so.
The entire codebase is <200 LOC which should make contributing a smooth ride.
