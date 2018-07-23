(ns cljdoc.clojars-stats.api
  (:require [cljdoc.clojars-stats.db :as db]
            [ring.middleware.params :as params]
            [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [reitit.ring.coercion]
            [reitit.coercion.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [muuntaja.middleware]))

(def exposed-queries
  ;; NEVER change those map entries, only add new stuff as needed
  {"/artifact-monthly" {:query-fn db/artifact-monthly
                        :summary "get downloads per timespan"
                        :params {:group_id string?
                                 :artifact_id string?}}})

(defn make-handler [db]
  (ring/ring-handler
    (ring/router
      [["/swagger.json" {:get {:no-doc true
                               :handler (swagger/create-swagger-handler)}}]
       (for [[path {:keys [query-fn params summary]}] exposed-queries]
         [path {:get {:parameters {:query params}
                      :swagger {:summary summary}
                      :handler (fn [request]
                                 {:status 200
                                  :body (query-fn db (get-in request [:parameters :query]))})}}])]
      {:data {:coercion reitit.coercion.spec/coercion
              :middleware [muuntaja.middleware/wrap-format
                           reitit.ring.coercion/coerce-exceptions-middleware
                           reitit.ring.coercion/coerce-request-middleware]
              :swagger {:id ::clojars-stats
                        :info {:title "Clojars Stats API"
                               :description "Contribute more at https://github.com/cljdoc/clojars-stats"}}}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler {:path "/"
                                             :config {:doc-expansion "list"}})
      (ring/create-default-handler))))

(defn start! [{:keys [db port]}]
  (assert (and db port))
  (-> (make-handler db)
      (params/wrap-params)
      (jetty/run-jetty {:port port :join? false})))

(comment
  (def db {:classname   "org.sqlite.JDBC"
           :subprotocol "sqlite"
           :subname     "clojars-stats.db"})

  (def srv (start! {:db db :port 3000}))

  (.stop srv)

  )
