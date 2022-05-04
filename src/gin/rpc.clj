(ns gin.rpc
  (:require [farseer.http :as http]
            [integrant.core :as ig]
            [gin.rpc.metastore :as metastore]
            [gin.rpc.hive :as hive]
            [gin.rpc.hdfs :as hdfs]))

(defn- f-chain  [coll f]
  (when-not (nil? coll)
    (when-let [m (f coll)]
      (merge coll m))))

(defn table-describe-rpc
  [{:keys [metastore-ds]} [db-name tbl-name]]
  (reduce f-chain
          {:DB_NAME db-name
           :TBL_NAME tbl-name}
          [#(metastore/table-describe metastore-ds %)
           #(hash-map :PART_SPEC
                      (metastore/partition-spec metastore-ds %))]))

(defn partition-list-rpc
  [{:keys [metastore-ds]} [tbl-id n]]
  (for [pm (metastore/partition-list metastore-ds {:TBL_ID tbl-id
                                                   :LIMIT n})
        :let [sm (metastore/partition-vals-spec metastore-ds pm)]]
    (assoc pm :PART_SPEC_VAL sm)))

(defn partition-find-rpc
  [{:keys [metastore-ds]} [tbl-id args]]
  (reduce f-chain
          {:TBL_ID tbl-id
           :PART_KEY_VAL_STR (apply str args)}
          [#(metastore/partition-find metastore-ds %)
           #(hash-map :PART_SPEC_VAL
                      (metastore/partition-vals-spec metastore-ds %))]))

(defn partition-parent-rpc
  [{:keys [metastore-ds]} [tbl-id ts]]
  (reduce f-chain
          {:TBL_ID tbl-id
           :CREATE_TIME ts}
          [#(metastore/partition-parent metastore-ds %)
           #(hash-map :PART_SPEC_VAL
                      (metastore/partition-vals-spec metastore-ds %))]))

(defn partition-attach-rpc
  [{:keys [metastore-ds hive-ds hadoop-ctx]} [tbl-id args src]]
  (reduce #(merge %1 (%2 %1))
          {:TBL_ID tbl-id
           :PART_KEY_VALS args
           :DATA_LOCATION src}
          [#(metastore/partition-describe metastore-ds %)
           #(hdfs/load-partition hadoop-ctx %)
           #(hive/add-partition hive-ds %)]))

(defn partition-detach-rpc
  [{:keys [metastore-ds hive-ds hadoop-ctx]} [tbl-id args]]
  (reduce #(merge %1 (%2 %1))
          {:TBL_ID tbl-id
           :PART_KEY_VAL_STR (apply str args)}
          [#(metastore/partition-find metastore-ds %)
           #(hash-map :PART_SPEC_VAL
                      (metastore/partition-vals-spec metastore-ds %))
           #(hive/drop-partition hive-ds %)
           #(hdfs/unload-partition hadoop-ctx %)]))
