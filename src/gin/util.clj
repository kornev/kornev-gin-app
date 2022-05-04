(ns gin.util
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [integrant.core :as ig]))

(defn- read-env
  [f s]
  (cond
    (symbol? s) (f (System/getenv (name s)))
    (string? s) (f (System/getenv s))
    :else (throw (ex-info "Wrong data type passed; Expected: Symbol, String" {:name s}))))

(defn env->str
  [s]
  (read-env identity s))

(defn env->int
  [s]
  (read-env #(Integer/parseInt %) s))

(defn read-system
  [s]
  (->> s io/resource slurp (edn/read-string {:readers
                                             {'str env->str
                                              'int env->int
                                              'ref ig/ref}})))
