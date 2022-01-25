(ns gin.storage.metastore
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [honey.sql :as sql]
            [honey.sql.helpers :refer [select from where join
                                       join-by order-by limit]]))

(defn- table-describe-q
  [sql-params]
  (-> (select :t/DB_ID
              :t/TBL_ID
              :t/SD_ID
              :s/CD_ID
              :d/DB_LOCATION_URI
              :s/LOCATION)
      (from [:DBS :d])
      (join-by :inner [[:TBLS :t] [:= :d/DB_ID :t/DB_ID]] :inner [[:SDS :s] [:= :t/SD_ID :s/SD_ID]])
      (where [:= :t/TBL_TYPE [:inline "EXTERNAL_TABLE"]]
             [:= :d/NAME [:param :DBS_NAME]]
             [:= :t/TBL_NAME [:param :TBL_NAME]])
      (sql/format {:quoted true
                   :params sql-params})))

(defn table-describe
  [connectable sql-params]
  (jdbc/execute-one! connectable
                     (table-describe-q sql-params)
                     {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-list-q
  [sql-params]
  (-> (select :p/TBL_ID
              :p/SD_ID
              :p/PART_ID
              :p/CREATE_TIME
              :s/CD_ID
              :p/PART_NAME
              :s/LOCATION)
      (from [:PARTITIONS :p])
      (join [:SDS :s] [:= :p/SD_ID :s/SD_ID])
      (where := :p/TBL_ID [:param :TBL_ID])
      (order-by [:p/CREATE_TIME :desc])
      (limit [:param :Q_LIMIT])
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-list
  [connectable sql-params]
  (jdbc/execute! connectable
                 (partition-list-q sql-params)
                 {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-schema-q
  [sql-params]
  (-> (select :v/INTEGER_IDX
              :k/PKEY_NAME
              :k/PKEY_TYPE
              :v/PART_KEY_VAL)
      (from [:PARTITIONS :p])
      (join-by :inner [[:PARTITION_KEYS :k] [:= :p/TBL_ID :k/TBL_ID]]
               :inner [[:PARTITION_KEY_VALS :v] [:= :p/PART_ID :v/PART_ID]])
      (where [:= :p/TBL_ID [:param :TBL_ID]]
             [:= :p/PART_ID [:param :PART_ID]]
             [:= :v/INTEGER_IDX :k/INTEGER_IDX])
      (order-by :k/INTEGER_IDX)
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-schema
  [connectable sql-params]
  (jdbc/execute! connectable
                 (partition-schema-q sql-params)
                 {:builder-fn result-set/as-unqualified-maps}))

(defn- partition-previous-q
  [sql-params]
  (-> (select :p/TBL_ID
              :p/SD_ID
              :p/PART_ID
              :p/CREATE_TIME
              :s/CD_ID
              :p/PART_NAME
              :s/LOCATION)
      (from [:PARTITIONS :p])
      (join [:SDS :s] [:= :p/SD_ID :s/SD_ID])
      (where [:= :p/TBL_ID [:param :TBL_ID]]
             [:< :p/CREATE_TIME [:param :CREATE_TIME]
                 ;(-> (select :CREATE_TIME)
                 ;    (from :PARTITIONS)
                 ;    (where [:= :TBL_ID [:param :TBL_ID]]
                 ;           [:= :PART_NAME [:param :PART_NAME]]))
              ])
      (order-by [:p/CREATE_TIME :desc])
      (limit [:inline 1])
      (sql/format {:quoted true
                   :params sql-params})))

(defn partition-previous
  [connectable sql-params]
  (jdbc/execute-one! connectable
                     (partition-previous-q sql-params)
                     {:builder-fn result-set/as-unqualified-maps}))
