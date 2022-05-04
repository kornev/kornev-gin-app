(ns gin.rpc.hdfs
  (:require [clojure.string :as str]
            [gin.ioc.jmx :as jmx])
  (:import (org.apache.hadoop.fs FileSystem
                                 Path)))

(defn- move
  [^FileSystem fs ^Path src ^Path dst]
  (. fs mkdirs dst)
  (. fs delete dst true)
  (. fs rename src dst))

(defn- unlink
  [^FileSystem fs ^Path src]
  (. fs delete src true))

(defn- path
  [^String s]
  (. Path getPathWithoutSchemeAndAuthority (Path. s)))

(defn- partition-dir
  [state]
  (let [tbl-home (:LOCATION state)
        cols (:PART_COLS state)
        vals (:PART_KEY_VALS state)]
    (->> (map #(str %1 "=" %2) cols vals)
         (str/join "/")
         (str tbl-home "/"))))

(defn load-partition
  [hadoop-ctx state]
  (let [src (-> (:DATA_LOCATION state) path)
        dst (-> (partition-dir state) path)]
    (move (jmx/active-fs hadoop-ctx) src dst)
    {:PART_LOCATION_MOVE (. dst toString)}))

(defn unload-partition
  [hadoop-ctx state]
  (let [src (-> state :LOCATION path)]
    (unlink (jmx/active-fs hadoop-ctx) src)
    {:PART_LOCATION_UNLINK (. src toString)}))
