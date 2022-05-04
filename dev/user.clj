(ns user
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [integrant.core :as ig]
            [integrant.repl :as stage]
            [farseer.client :as client]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [honey.sql :as sql]
            [fipp.edn :refer [pprint] :rename {pprint echo}]
            [gin.rpc :as rpc]
            [gin.ioc.jmx :as jmx]
            [gin.rpc.hdfs :as hdfs])
  (:import (org.apache.hadoop.fs FileSystem
                                 Path)))

;;; GENERAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-system
  [m]
  (integrant.repl/set-prep! #(ig/prep m))
  (ig/load-namespaces m))

(defn find-node
  [k]
  (second (ig/find-derived-1 integrant.repl.state/system k)))

;;; JSON-RPC CLIENT ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private call nil)

(defn make-client
  [m]
  (let [{:keys [host port]} (-> m :gin.ioc/http :jetty-spec)
        client (client/make-client {:http/url (str "http://" host ":" port "/rpc")})]
    (alter-var-root #'call
                    (constantly (partial client/call client)))))

;;; APP ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config
 {:gin.ioc/http {:jetty-spec {:host "127.0.0.1"
                              :port 8080}
                 :handler (ig/ref :gin.ioc/rpc)}
  :gin.ioc/rpc {:metastore (ig/ref :gin.ioc/metastore)
                :hive (ig/ref :gin.ioc/hive)
                :hdfs (ig/ref :gin.ioc/hdfs)}
  :gin.ioc/hdfs {:hadoop-spec {:namenodes [{:host "192.168.7.111"
                                            :fs-metadata-port 8020
                                            :web-ui-port 50070}
                                           {:host "192.168.7.112"
                                            :fs-metadata-port 8020
                                            :web-ui-port 50070}]
                               :user "pmp"
                               :props {"fs.permissions.umask-mode" "0002"}}}
  :gin.ioc/metastore {:jdbc-spec {:dbtype "postgres"
                                  :dbname "hive"
                                  :host "192.168.7.111"
                                  :port 5432
                                  :user "hive"
                                  :password "IOzC8S0lubO1H3T6TBbr2c5GtGKK62V5"}
                      :hikari-spec {:minimumIdle 1}}
  :gin.ioc/hive {:jdbc-spec {:classname "org.apache.hive.jdbc.HiveDriver"
                             :dbtype "hive2"
                             :host "192.168.7.112"
                             :port 10000
                             :user "pmp"
                             :password "pmp"}
                 :hikari-spec {:minimumIdle 1}}})

(defn start
  [m]
  (make-system m)
  (make-client m)
  (stage/go))

(defn metastore!
  [q & [p & r]]
  (jdbc/execute! (find-node :gin.ioc/metastore)
                 (sql/format q {:quoted true
                                :params p})
                 {:builder-fn result-set/as-unqualified-maps}))

(defn hive!
  [& more]
  (jdbc/execute! (find-node :gin.ioc/hive)
                 more
                 {:builder-fn result-set/as-unqualified-maps}))

(defn file [path data]
  (let [fs (jmx/active-fs
            (find-node :gin.ioc/hdfs))
        path (Path. path)]
    (when (. fs exists path)
      (. fs delete path))
    (doto (. fs create path true)
      (.writeBytes data)
      (.close))))

;;; TEST ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table-describe-rpc
  [& r]
  (echo
   (rpc/table-describe-rpc {:metastore-ds (find-node :gin.ioc/metastore)
                            :hive-ds (find-node :gin.ioc/hive)
                            :hadoop-ctx (find-node :gin.ioc/hdfs)}
                           r)))

(defn table-describe
  [& r]
  (echo
   (call :table/describe r)))

(defn partition-list
  [& r]
  (echo
   (call :partition/list r)))

(defn partition-find
  [& r]
  (echo
   (call :partition/find r)))

(defn partition-parent
  [& r]
  (echo
   (call :partition/parent r)))

(defn partition-attach
  [& r]
  (echo
   (call :partition/attach r)))

(defn partition-detach
  [& r]
  (echo
   (call :partition/detach r)))

;;; RUN ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(start config)

(table-describe-rpc #_(DB_NAME) "processing"
                    #_(TBL_NAME) "test01")

(partition-list #_(TBL_ID) 198
                #_(LIMIT) 2)

(partition-find #_(TBL_ID) 248
                #_(PART_KEY_VAL_STR) ["2021" "08" "09" "15"])

(partition-parent #_(TBL_ID) 59
                  #_(CREATE_TIME) 1626746198)

;curl -s -X POST 'http://127.0.0.1:8080/rpc' \
;  -d '{"id": 61966, "jsonrpc": "2.0", "method": "partition/attach", "params": [248, ["2021", "08", "09", "15"]]}' \
;  -H 'content-type: application/json'

(do
  (file "/user/pmp/input/data.csv" "3,omar\n4,lory")
  (partition-attach #_(TBL_ID) 261
                    #_(PART_KEY_VALS) ["2019-11-23" "moscow" 13]
                    #_(DATA_LOCATION) "/user/pmp/input"))
(echo
 (hive! "show partitions processing.test01"))
(echo
 (hive! "select * from processing.test01 where num=13"))

(partition-detach #_(TBL_ID) 261
                  #_(PART_KEY_VALS) ["2019-11-23" "moscow" 13])

;(echo
; (gin.rpc.metastore/partition-describe
;  (find-node :gin.ioc/metastore)
;  {:TBL_ID 261}))

;(echo
; (metastore! {:select [:datname]
;              :from [[:pg_database :d]]
;              :where [:= :d/datistemplate false]}))

;(echo
; (metastore! {:select [:table_schema
;                       :table_name]
;              :from [:information_schema.tables]
;              :order-by [:table_schema :table_name]}))

;(echo
; (metastore! {:select [:*] :from [:DBS] :limit 3}))
