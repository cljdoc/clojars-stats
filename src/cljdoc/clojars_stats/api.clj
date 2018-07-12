(ns cljdoc.clojars-stats.api
  (:require [cljdoc.clojars-stats.db :as db]
            [ring.middleware.params :as params]
            [ring.adapter.jetty :as jetty]
            [jsonista.core :as j]))

(def exposed-queries
  {"/artifact-monthly" {:query-fn db/artifact-monthly
                        :params [:group_id :artifact_id]}})

(defn make-handler [db]
  (fn handler [{:keys [uri query-params]}]
    (if-let [{:keys [query-fn params]} (get exposed-queries uri)]
      (if (every? query-params (map name params))
        {:status 200
         :body (->> (for [p params] [p (get query-params (name p))])
                    (into {})
                    (query-fn db)
                    (j/write-value-as-string))}
        {:status 400
         :body (format "Insufficient parameters:\nProvided: %s\nExpected: %s" query-params params)})
      {:status 404
       :body (format "Unknown query: %s" uri)})))

(defn start! [{:keys [db port]}]
  (assert (and db port))
  (-> (make-handler db)
      (params/wrap-params)
      (jetty/run-jetty {:port port :join? false})))

(comment

  (def srv (start! cljdoc.clojars-stats/db))

  (.stop srv)

  (every? {"group_id" 1} ["group_id" "artifact_id"])

  (let [query-params {"group_id" 1 "artifact_id" 2}
        params [:group_id :artifact_id]]

    )



  )
