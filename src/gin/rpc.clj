(ns gin.rpc
  (:require [farseer.http :as http]
            [integrant.core :as ig]
            [gin.storage.metastore :as metastore]
            [gin.storage.hive :as hive]
            [gin.storage.hdfs :as hdfs]))

(defn rpc-tab-desc
  [{:keys [metastore-ds]} [db-name tbl-name]]
  (reduce #(merge %1 (%2 %1))
          {:DB_NAME  db-name
           :TBL_NAME tbl-name}
          [#(metastore/tab-desc metastore-ds %)
           #(hash-map :PART_SPEC
                      (metastore/part-spec metastore-ds %))]))

(defn rpc-part-head
  [{:keys [metastore-ds]} [tbl-id n]]
  (for [pm (metastore/part-head metastore-ds {:TBL_ID tbl-id
                                              :LIMIT  n})
        :let [sm (metastore/part-spec-vals metastore-ds pm)]]
    (assoc pm :PART_SPEC sm)))

(defn rpc-part-find
  [{:keys [metastore-ds]} [tbl-id args]]
  (reduce #(merge %1 (%2 %1))
          {:TBL_ID           tbl-id
           :PART_KEY_VAL_STR (apply str args)}
          [#(metastore/part-find metastore-ds %)
           #(hash-map :PART_SPEC_VAL
                      (metastore/part-spec-vals metastore-ds %))]))

(defn rpc-part-prev
  [{:keys [metastore-ds]} [tbl-id ts]]
  (reduce #(merge %1 (%2 %1))
          {:TBL_ID      tbl-id
           :CREATE_TIME ts}
          [#(metastore/part-prev metastore-ds %)
           #(hash-map :PART_SPEC_VAL
                      (metastore/part-spec-vals metastore-ds %))]))

(defn rpc-part-mark
  [{:keys [metastore-ds hive-ds hadoop-fs]} [tbl-id args src]]
  (reduce #(merge %1 (%2 %1))
          {:TBL_ID        tbl-id
           :PART_KEY_VALS args
           :DATA_LOCATION src}
          [#(metastore/part-desc metastore-ds %)
           #(hdfs/upload-partition hadoop-fs %)
           #(hive/add-partition hive-ds %)]))

(defn rpc-part-drop
  [{:keys [metastore-ds hive-ds hadoop-fs]} [tbl-id args]]
  (reduce #(merge %1 (%2 %1))
          {:TBL_ID           tbl-id
           :PART_KEY_VAL_STR (apply str args)}
          [#(metastore/part-find metastore-ds %)
           #(hash-map :PART_SPEC_VAL
                      (metastore/part-spec-vals metastore-ds %))
           #(hive/drop-partition hive-ds %)
           #(hdfs/unload-partition hadoop-fs %)]))

(def ^:private farseer-default-spec
  {:http/path    "/rpc"
   :rpc/handlers {:tab/desc  {:handler/function #'rpc-tab-desc}
                  :part/head {:handler/function #'rpc-part-head}
                  :part/find {:handler/function #'rpc-part-find}
                  :part/prev {:handler/function #'rpc-part-prev}
                  :part/mark {:handler/function #'rpc-part-mark}
                  :part/drop {:handler/function #'rpc-part-drop}}})

(defmethod ig/init-key ::handler [_ {:keys [metastore hive hdfs farseer-spec]}]
  (http/make-app (merge farseer-default-spec
                        farseer-spec)
                 {:metastore-ds metastore
                  :hive-ds      hive
                  :hadoop-fs    hdfs}))
