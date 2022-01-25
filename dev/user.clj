(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as stage]
            [farseer.client :as client]
            [fipp.edn :refer [pprint] :rename {pprint echo}]))

(def ^:private rpc nil)

(defn make-client [m]
  (let [{:keys [host port]} (-> m :gin.http/server :options)
        client (client/make-client {:http/url (str "http://" host ":" port "/rpc")})]
    (alter-var-root #'rpc
                    (constantly (partial client/call client)))))

(defn make-system [m]
  (integrant.repl/set-prep! #(ig/prep m))
  (ig/load-namespaces m))

(defn find-node [k]
  (second (ig/find-derived-1 integrant.repl.state/system k)))

(def config
  {:gin.http/server       {:options {:host "127.0.0.1"
                                     :port 8080}
                           :handler (ig/ref :gin.rpc/handler)}
   :gin.rpc/handler       {:metastore (ig/ref :gin.storage/metastore)}
   :gin.storage/metastore {:server-name "???"
                           :port-number 5432
                           :username "hive"
                           :password "???"}})

(make-system config)
(make-client config)

(stage/go)

(echo
 (rpc :table/describe [#_(database) "processing" #_(table) "user_agg"]))

(echo
 (rpc :partition/list [#_(TBL_ID) 59 #_(Q_LIMIT) 2]))

(echo
 (rpc :partition/previous [#_(TBL_ID) 59 #_(CREATE_TIME) 1626746198]))
