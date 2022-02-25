(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as stage]
            [farseer.client :as client]
            [fipp.edn :refer [pprint] :rename {pprint echo}]))

(def ^:private rpc nil)

(defn mk-client
  [m]
  (let [{:keys [host port]} (-> m :gin.http/server :jetty-spec)
        client (client/make-client {:http/url (str "http://" host ":" port "/rpc")})]
    (alter-var-root #'rpc
                    (constantly (partial client/call client)))))

(defn mk-system
  [m]
  (integrant.repl/set-prep! #(ig/prep m))
  (ig/load-namespaces m))

(defn find-node
  [k]
  (second (ig/find-derived-1 integrant.repl.state/system k)))

(def config
  {:gin.http/server       {:jetty-spec {:host "127.0.0.1"
                                        :port 8080}
                           :handler (ig/ref :gin.rpc/handler)}
   :gin.rpc/handler       {:metastore (ig/ref :gin.storage/metastore)
                           :hive (ig/ref :gin.storage/hive)
                           :hdfs (ig/ref :gin.storage/hdfs)}
   :gin.storage/metastore {:jdbc-spec {:dbtype "postgres"
                                       :dbname "hive"
                                       :host "192.168.7.111"
                                       :port 5432
                                       :user "hive"
                                       :password "IOzC8S0luaO1H3T6TBbr3c4GtGKK62V6"}
                           :hikari-spec {:minimumIdle 1}}
   :gin.storage/hive {:jdbc-spec {:classname "org.apache.hive.jdbc.HiveDriver"
                                  :dbtype "hive2"
                                  :host "192.168.7.112"
                                  :port 10000
                                  :user "pmp"
                                  :password "pmp"}
                      :hikari-spec {:minimumIdle 1}}
   :gin.storage/hdfs {:hadoop-spec {"fs.defaultFS" "hdfs://192.168.7.112:8020/user/pmp"
                                    "fs.permissions.umask-mode" "0002"
                                    "hadoop.job.ugi" "pmp"}}})

(defn start
  []
  (mk-system config)
  (mk-client config)
  (stage/go))

(start)

(echo
 (rpc :tab/desc [#_(DBS_NAME) "processing" #_(TBL_NAME) "test01"]))

(echo
 (rpc :part/head [#_(TBL_ID) 198 #_(LIMIT) 2]))

(echo
 (rpc :part/find [#_(TBL_ID) 248 #_(PART_KEY_VAL_STR) ["2021" "08" "09" "15"]]))

(echo
 (rpc :part/prev [#_(TBL_ID) 59 #_(CREATE_TIME) 1626746198]))

#_(echo
 (rpc :part/mark [#_(TBL_ID) 261
                  #_(PART_KEY_VALS) ["2019-11-22" "moscow" 13]
                  #_(DATA_LOCATION) "/user/pmp/input"]))

#_(echo
 (rpc :part/drop [#_(TBL_ID) 261
                  #_(PART_KEY_VALS) ["2019-11-22" "moscow" 13]]))
