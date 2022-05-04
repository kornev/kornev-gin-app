(ns gin.rpc.metastore
  (:refer-clojure :exclude [partition-by])
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [honey.sql :as sql]
            [honey.sql.helpers :refer [with select select-distinct-on from
                                       join join-by where over order-by
                                       partition-by window limit]]
            [gin.log :refer [wrap-log-execute]])
  (:import (java.sql Array)))

(extend-protocol result-set/ReadableColumn
  Array
  (read-column-by-label [^Array v _] (vec (.getArray v)))
  (read-column-by-index [^Array v _ _] (vec (.getArray v))))

(defn- table-describe-q
  [sql-params]
  (-> (select :d/DB_ID
              [:d/NAME :DB_NAME]
              :d/DB_LOCATION_URI
              :t/TBL_ID
              :t/TBL_NAME
              :s/SD_ID
              :s/CD_ID
              :s/LOCATION)
      (from [:DBS :d])
      (join-by :inner [[:TBLS :t] [:= :t/DB_ID :d/DB_ID]]
               :inner [[:SDS :s] [:= :s/SD_ID :t/SD_ID]])
      (where [:= :t/TBL_TYPE [:inline "EXTERNAL_TABLE"]]
             [:= :d/NAME [:param :DB_NAME]]
             [:= :t/TBL_NAME [:param :TBL_NAME]])
      (sql/format {:quoted true
                   :params sql-params})))

(defn table-describe
  [connectable sql-params]
  (jdbc/execute-one! (wrap-log-execute connectable "metastore/table-describe")
                     (table-describe-q sql-params)
                     {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-spec-q
  [sql-params]
  (-> (select :INTEGER_IDX
              :PKEY_NAME
              :PKEY_TYPE)
      (from [:PARTITION_KEYS :k])
      (where := :k/TBL_ID [:param :TBL_ID])
      (order-by :k/INTEGER_IDX)
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-spec
  [connectable sql-params]
  (jdbc/execute! (wrap-log-execute connectable "metastore/partition-spec")
                 (partition-spec-q sql-params)
                 {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-list-q
  [sql-params]
  (-> (select :d/DB_ID
              [:d/NAME :DB_NAME]
              :p/TBL_ID
              :t/TBL_NAME
              :p/PART_ID
              :p/PART_NAME
              :p/CREATE_TIME
              :p/SD_ID
              :s/CD_ID
              :s/LOCATION)
      (from [:PARTITIONS :p])
      (join-by :inner [[:SDS :s] [:= :s/SD_ID :p/SD_ID]]
               :inner [[:TBLS :t] [:= :t/TBL_ID :p/TBL_ID]]
               :inner [[:DBS :d] [:= :d/DB_ID :t/DB_ID]])
      (where := :p/TBL_ID [:param :TBL_ID])
      (order-by [:p/CREATE_TIME :desc])
      (limit [:param :LIMIT])
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-list
  [connectable sql-params]
  (jdbc/execute! (wrap-log-execute connectable "metastore/partition-list")
                 (partition-list-q sql-params)
                 {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-vals-spec-q
  [sql-params]
  (-> (select :k/INTEGER_IDX
              :k/PKEY_NAME
              :k/PKEY_TYPE
              :v/PART_KEY_VAL)
      (from [:PARTITIONS :p])
      (join-by :inner [[:PARTITION_KEYS :k] [:= :k/TBL_ID :p/TBL_ID]]
               :inner [[:PARTITION_KEY_VALS :v] [:= :v/PART_ID :p/PART_ID]])
      (where [:= :p/TBL_ID [:param :TBL_ID]]
             [:= :p/PART_ID [:param :PART_ID]]
             [:= :v/INTEGER_IDX :k/INTEGER_IDX])
      (order-by :k/INTEGER_IDX)
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-vals-spec
  [connectable sql-params]
  (jdbc/execute! (wrap-log-execute connectable "metastore/partition-vals-spec")
                 (partition-vals-spec-q sql-params)
                 {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-find-q
  [sql-params]
  (let [part-col-vals-transit-t
        (-> (select :v/*
                    (over [[:string_agg :PART_KEY_VAL [:inline ""]] :PART_W :PART_KEY_VAL_STR]))
            (from [:PARTITION_KEY_VALS :v])
            (window :PART_W (-> (partition-by :PART_ID)
                                (order-by :INTEGER_IDX))))
        part-col-vals-t
        (-> (select-distinct-on [:v/PART_ID] :v/PART_ID
                                :v/PART_KEY_VAL_STR)
            (from [:PART_COL_VALS_TRANSIT_T :v])
            (order-by :v/PART_ID [:v/PART_KEY_VAL_STR :desc]))]
    (-> (with [:PART_COL_VALS_TRANSIT_T part-col-vals-transit-t]
              [:PART_COL_VALS_T part-col-vals-t])
        (select :p/TBL_ID
                :p/SD_ID
                :p/PART_ID
                :p/CREATE_TIME
                :s/CD_ID
                [:d/NAME :DB_NAME]
                :t/TBL_NAME
                :p/PART_NAME
                :s/LOCATION)
        (from [:PART_COL_VALS_T :v])
        (join-by :inner [[:PARTITIONS :p] [:= :p/PART_ID :v/PART_ID]]
                 :inner [[:SDS :s] [:= :s/SD_ID :p/SD_ID]]
                 :inner [[:TBLS :t] [:= :t/TBL_ID :p/TBL_ID]]
                 :inner [[:DBS :d] [:= :d/DB_ID :t/DB_ID]])
        (where [:= :p/TBL_ID [:param :TBL_ID]]
               [:= :t/TBL_TYPE [:inline "EXTERNAL_TABLE"]]
               [:= :v/PART_KEY_VAL_STR [:param :PART_KEY_VAL_STR]])
        (sql/format {:quoted true
                     :params sql-params}))))

(defn partition-find
  [connectable sql-params]
  (jdbc/execute-one! (wrap-log-execute connectable "metastore/partition-find")
                     (partition-find-q sql-params)
                     {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-parent-q
  [sql-params]
  (-> (select :d/DB_ID
              [:d/NAME :DB_NAME]
              :p/TBL_ID
              :t/TBL_NAME
              :p/PART_ID
              :p/PART_NAME
              :p/CREATE_TIME
              :p/SD_ID
              :s/CD_ID
              :s/LOCATION)
      (from [:PARTITIONS :p])
      (join-by :inner [[:SDS :s] [:= :s/SD_ID :p/SD_ID]]
               :inner [[:TBLS :t] [:= :t/TBL_ID :p/TBL_ID]]
               :inner [[:DBS :d] [:= :d/DB_ID :t/DB_ID]])
      (where [:= :p/TBL_ID [:param :TBL_ID]]
             [:< :p/CREATE_TIME [:param :CREATE_TIME]])
      (order-by [:p/CREATE_TIME :desc])
      (limit [:inline 1])
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-parent
  [connectable sql-params]
  (jdbc/execute-one! (wrap-log-execute connectable "metastore/partition-parent")
                     (partition-parent-q sql-params)
                     {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-describe-q
  [sql-params]
  (let [part-cols-types-t
        (-> (select-distinct-on [:TBL_ID]
                                (over [[:array_agg :PKEY_NAME] :TBL_W :PART_COLS])
                                (over [[:array_agg :PKEY_TYPE] :TBL_W :PART_TYPES])
                                :TBL_ID)
            (from :PARTITION_KEYS)
            (order-by :TBL_ID [:PART_COLS :desc] [:PART_TYPES :desc])
            (window :TBL_W (-> (partition-by :TBL_ID)
                               (order-by :INTEGER_IDX))))]
    (-> (with [:PART_COLS_TYPES_T part-cols-types-t])
        (select :d/DB_ID
                [:d/NAME :DB_NAME]
                :t/TBL_ID
                :t/TBL_NAME
                :c/PART_COLS
                :c/PART_TYPES
                :s/SD_ID
                :s/CD_ID
                :s/LOCATION)
        (from [:TBLS :t])
        (join-by :inner [[:DBS :d] [:= :d/DB_ID :t/DB_ID]]
                 :inner [[:SDS :s] [:= :s/SD_ID :t/SD_ID]]
                 :inner [[:PART_COLS_TYPES_T :c] [:= :c/TBL_ID :t/TBL_ID]])
        (where [:= :t/TBL_TYPE [:inline "EXTERNAL_TABLE"]]
               [:= :t/TBL_ID [:param :TBL_ID]])
        (sql/format {:quoted true
                     :params sql-params}))))

(defn partition-describe
  [connectable sql-params]
  (jdbc/execute-one! (wrap-log-execute connectable "metastore/partition-describe")
                     (partition-describe-q sql-params)
                     {:builder-fn result-set/as-unqualified-maps}))
