(ns muotti.malli
  (:require [malli.core :as malli]
            [malli.transform :as mt]
            [muotti.core :as muotti]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

; Malli native types are :any, :nil, :string, :int, :double, :boolean, :keyword, :qualified-keyword, :symbol, :qualified-symbol, and :uuid
; Supporting :nil is kinda nonsensical, all others are somewhat doable
(def malli-config {:transformations {;malli.core/base-schemas
                                     [:string :int]               {:transformer parse-long}
                                     [:string :double]            {:transformer parse-double}
                                     [:string :boolean]           {:transformer parse-boolean}
                                     [:string :keyword]           {:transformer keyword}
                                     [:string :symbol]            {:transformer symbol}
                                     [:string :uuid]              {:transformer parse-uuid}
                                     [:int :string]               {:transformer str}
                                     [:int :double]               {:transformer double}
                                     [:double :string]            {:transformer str}
                                     [:boolean :string]           {:transformer str}
                                     [:keyword :string]           {:transformer name}
                                     [:keyword :symbol]           {:transformer symbol}
                                     [:qualified-keyword :string] {:transformer (comp (partial str/join "/") (juxt namespace name))}
                                     [:symbol :string]            {:transformer str}
                                     [:qualified-symbol :string]  {:transformer str}
                                     [:uuid :string]              {:transformer str}
                                     [:int :any]                  {:transformer identity}
                                     [:double :any]               {:transformer identity}
                                     [:boolean :any]              {:transformer identity}
                                     [:keyword :any]              {:transformer identity}
                                     [:qualified-keyword :any]    {:transformer identity}
                                     [:symbol :any]               {:transformer identity}
                                     [:qualified-symbol :any]     {:transformer identity}
                                     [:uuid :any]                 {:transformer identity}

                                     ; malli.core/predicate-schemas
                                     [:string pos?]               {:transformer parse-long}
                                     [:int pos?]                  {:transformer identity}
                                     [:double pos?]               {:transformer identity}}})

(def supported-types (->> malli-config :transformations keys flatten set))

(defn ^:private detect-type
  [_ v]
  (let [assumed-type (cond
                       (string? v)            :string
                       (integer? v)           :int
                       (double? v)            :double
                       (boolean? v)           :boolean
                       (qualified-keyword? v) :qualified-keyword
                       (keyword? v)           :keyword
                       (qualified-symbol? v)  :qualified-symbol
                       (symbol? v)            :symbol
                       (uuid? v)              :uuid
                       :else                  ::unsupported)]
    (log/tracef "Value %s seems to be of type %s" v assumed-type)
    assumed-type))

(defmulti schema-matcher #(malli/-type %))

(defn ^:private converter
  [transformer from-fn to-fn props]
  (fn [x]
    (let [source-type (from-fn x)
          target-type (to-fn x)
          ignore? (some-> props :muotti/ignore)
          log-context (fn [] (format "[%s -> %s] conversion of %s" source-type target-type x))]

      (if (true? ignore?)
        (do
          (log/debugf "%s ignored" (log-context))
          x)
        (if-not (some #{target-type} supported-types)                ; TODO: better detection for target schema type support
          (do
            (log/warnf "%s not supported: unsupported target type" (log-context))
            x)
          (if (and (keyword? (-> target-type))
                   (not= source-type target-type)
                   (and (not= ::unsupported source-type)
                        (not= ::unsupported target-type)))
            (do (log/infof "%s supported" (log-context))
                (muotti/transform transformer source-type target-type x))
            x))))))

(defn ^:private ->malli-xformer
  [muotti-transformer from-fn to-fn]
  (fn [schema _options]
    (println (str "<<< " (pr-str schema) " / " (malli/-properties schema)))
    (println (str "<<< " _options))
    (converter
      muotti-transformer
      (partial from-fn schema)
      (partial to-fn schema)
      (malli/-properties schema))))

(defn transformer
  ([muotti-transformer]
   (transformer muotti-transformer muotti-transformer))
  ([muotti-decoder muotti-encoder]
   (letfn [(from-schema [schema _] (println "resolving schema to malli form") (malli/-form schema))]
     (mt/transformer
       {:name            :muotti
        :default-decoder {:compile (->malli-xformer muotti-decoder detect-type from-schema)}
        :default-encoder {:compile (->malli-xformer muotti-encoder from-schema detect-type)}}))))
