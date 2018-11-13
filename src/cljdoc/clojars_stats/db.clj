(ns cljdoc.clojars-stats.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "queries.sql")
(hugsql/def-sqlvec-fns "queries.sql")

(comment
  (artifact-monthly cljdoc.clojars-stats/db {:group_id "reagent" :artifact_id "reagent"})
  )
