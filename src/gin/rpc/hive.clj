(ns gin.rpc.hive
  (:require [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [gin.log :refer [wrap-log-execute]]))

(def ^:private cast-f
  {"string" (fn [s] (str "'" s "'"))
   "date" (fn [s] (str "date '" s "'"))
   "int" str})

(defn- partition-vals
  [cols types vals]
  (->> (map #((get cast-f %1) %2) types vals)
       (map #(str %1 "=" %2) cols)
       (str/join ", ")))

(defn- add-partition-q
  [state]
  (let [db-name (:DB_NAME state)
        tbl-name (:TBL_NAME state)
        part-vals (partition-vals (:PART_COLS state)
                                  (:PART_TYPES state)
                                  (:PART_KEY_VALS state))]
    [(str "ALTER TABLE " db-name "." tbl-name " ADD IF NOT EXISTS PARTITION(" part-vals ")")]))

(defn add-partition
  [connectable state]
  (let [add-partition-sql (add-partition-q state)]
    (jdbc/execute-one! (wrap-log-execute connectable "hive/add-partition")
                       add-partition-sql
                       {:builder-fn result-set/as-unqualified-maps})
    {:ADD_PARTITION_SQL (first add-partition-sql)}))

(defn- drop-partition-q
  [state]
  (let [db-name (:DB_NAME state)
        tbl-name (:TBL_NAME state)
        part-vals (partition-vals (->> state :PART_SPEC_VAL (map :PKEY_NAME))
                                  (->> state :PART_SPEC_VAL (map :PKEY_TYPE))
                                  (->> state :PART_SPEC_VAL (map :PART_KEY_VAL)))]
    [(str "ALTER TABLE " db-name "." tbl-name " DROP IF EXISTS PARTITION(" part-vals ")")]))

(defn drop-partition
  [connectable state]
  (let [drop-partition-sql (drop-partition-q state)]
    (jdbc/execute-one! (wrap-log-execute connectable "hive/drop-partition")
                       drop-partition-sql
                       {:builder-fn result-set/as-unqualified-maps})
    {:DROP_PARTITION_SQL (first drop-partition-sql)}))
