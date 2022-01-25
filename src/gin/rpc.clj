(ns gin.rpc
  (:require [clojure.string :as srt]
            [farseer.http :as http]
            [integrant.core :as ig]
            [gin.storage.metastore :as metastore]))

(defn rpc-table-describe
  [{:keys [ds]} [a b]]
  (let [sql-params {:DBS_NAME a :TBL_NAME b}]
    (metastore/table-describe ds sql-params)))

(defn rpc-partition-list
  [{:keys [ds]} [a b]]
  (for [pm (metastore/partition-list ds {:TBL_ID a :Q_LIMIT b})
        :let [sm (metastore/partition-schema ds pm)]]
    (assoc pm :PART_SCHEMA sm)))

(defn rpc-partition-previous
  [{:keys [ds]} [a b]]
  (let [sql-params {:TBL_ID a :CREATE_TIME b}
        pm (metastore/partition-previous ds sql-params)
        sm (metastore/partition-schema ds pm)]
    (assoc pm :PART_SCHEMA sm)))

(def ^:private config
  {:http/path    "/rpc"
   :rpc/handlers {:table/describe {:handler/function #'rpc-table-describe}
                  :partition/list {:handler/function #'rpc-partition-list}
                  :partition/previous {:handler/function #'rpc-partition-previous}}})

(defmethod ig/init-key ::handler [_ {:keys [metastore options]}]
  (http/make-app (merge config options) {:ds metastore}))
