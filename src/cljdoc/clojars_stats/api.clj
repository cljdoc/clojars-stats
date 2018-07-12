(ns cljdoc.clojars-stats.api
  (:require [cljdoc.clojars-stats.db :as db]
            [ring.middleware.params :as params]
            [ring.adapter.jetty :as jetty]
            [jsonista.core :as j]
            [clojure.string :as string]))

(def exposed-queries
  ;; NEVER change those map entries, only add new stuff as needed
  {"/artifact-monthly" {:query-fn db/artifact-monthly
                        :params [:group_id :artifact_id]}})

(def index-page
  (->> ["<pre>"
        "Various endpoints are available:\n"
        (for [[k {:keys [params]}] exposed-queries]
          (str k " query-params: " (mapv name params) "\n"))
        "\nContribute more at https://github.com/cljdoc/clojars-stats"
        "</pre>"]
       flatten
       (string/join "\n")))

(defn make-handler [db]
  (fn handler [{:keys [uri query-params]}]
    (if (= "/" uri)
      {:status 200 :body index-page}
      (if-let [{:keys [query-fn params]} (get exposed-queries uri)]
        (if (every? query-params (map name params))
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (->> (for [p params] [p (get query-params (name p))])
                      (into {})
                      (query-fn db)
                      (j/write-value-as-string))}
          {:status 400
           :body (format "Insufficient parameters:\nProvided: %s\nExpected: %s" query-params params)})
        {:status 404
         :body (format "Unknown query: %s" uri)}))))

(defn start! [{:keys [db port]}]
  (assert (and db port))
  (-> (make-handler db)
      (params/wrap-params)
      (jetty/run-jetty {:port port :join? false})))

(comment

  (def srv (start! {:db cljdoc.clojars-stats/db :port 3000}))

  (.stop srv)

  (every? {"group_id" 1} ["group_id" "artifact_id"])

  (let [query-params {"group_id" 1 "artifact_id" 2}
        params [:group_id :artifact_id]]

    )



  )
