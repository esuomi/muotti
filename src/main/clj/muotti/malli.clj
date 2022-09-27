(ns muotti.malli
  (:require [malli.core :as malli]
            [malli.registry :as mr]
            [malli.transform :as mt]
            [muotti.core :as muotti]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (clojure.lang BigInt Ratio)))

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

                                     ; malli.core/predicate-schemas - extended types
                                     [:string ::big-integer]      {:transformer (fn [^String s] (BigInteger. s))}
                                     [:string ::ratio]            {:transformer (fn [^String s]
                                                                                  (->> (clojure.string/split s #"/" 2)
                                                                                       (mapv #(BigInteger. ^String %))
                                                                                       (apply #(Ratio. %1 %2))))}
                                     [:int ::big-integer]         {:transformer (fn [^String s] (BigInteger. s))}
                                     [:int ::big-decimal]         {:transformer bigdec}
                                     [:int ::float]               {:transformer float}
                                     [:int ::ratio]               {:transformer #(Ratio.
                                                                                   (. BigInteger (valueOf %))
                                                                                   (. BigInteger (valueOf %)))}
                                     ; malli.core/predicate-schemas - type test predicates
                                     [:int 'int?]                 {:validator int? :transformer identity}
                                     [:int 'integer?]             {:validator integer? :transformer identity}
                                     [:int 'nat-int?]             {:validator nat-int? :transformer identity}
                                     [:int 'number?]              {:validator number? :transformer identity}

                                     [::big-integer 'integer?]    {:validator integer? :transformer identity}
                                     [::big-integer 'nat-int?]    {:validator nat-int? :transformer identity}
                                     [::big-integer 'number?]     {:validator number? :transformer identity}

                                     [:double 'double?]           {:validator double? :transformer identity}
                                     [:double 'float?]            {:validator float? :transformer identity}
                                     [:double 'number?]           {:validator number? :transformer identity}

                                     [::big-decimal 'decimal?]    {:validator decimal? :transformer identity}
                                     [::big-decimal 'float?]      {:validator float? :transformer identity}
                                     [::big-decimal 'number?]     {:validator number? :transformer identity}

                                     [::float 'float?]            {:validator float? :transformer identity}
                                     [::float 'number?]           {:validator number? :transformer identity}

                                     [::ratio 'ratio?]            {:validator ratio? :transformer identity}
                                     [::ratio 'number?]           {:validator number? :transformer identity}
                                     ; malli.core/predicate-schemas - mathematical properties predicates
                                     ['int? 'pos-int?]            {:validator pos-int? :transformer identity}
                                     ['int? 'neg-int?]            {:validator neg-int? :transformer identity}

                                     ['integer? 'rational?]       {:validator rational? :transformer identity}
                                     ['ratio? 'rational?]         {:validator rational? :transformer identity}

                                     ['number? 'pos?]             {:validator pos? :transformer identity}
                                     ['number? 'neg?]             {:validator neg? :transformer identity}
                                     ['number? 'zero?]            {:validator zero? :transformer identity}

                                     ; unsupported due to ambiquity:
                                     ;  any?, some?,
                                     ; unsupported due to complex class relation:
                                     ;  uuid?, uri?, inst?,
                                     ; unsupported due to associative complexity:
                                     ;  seqable?, indexed?, map?, vector?, list?, seq?,
                                     ; maybe in the future:
                                     ;  ident?, simple-ident?, qualified-ident?, keyword?, simple-keyword?,
                                     ;  qualified-keyword?, symbol?, simple-symbol?, qualified-symbol?,
                                     ;  char?, set?, nil?, false?, true?, zero?, rational?, coll?, empty?, associative?,
                                     ;  sequential?, ratio?, bytes?, ifn? and fn?
                                     }})
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

(defn ^:private converter
  [transformer from-fn to-fn props supported-types]
  (fn [x]
    (let [source-type (or (some-> props :muotti/source) (from-fn x))
          target-type (or (some-> props :muotti/target) (to-fn x))
          ignore?     (some-> props :muotti/ignore)
          default     (some-> props :muotti/default)
          log-context (fn [] (format "[%s -> %s] conversion of %s" source-type target-type x))]
        (cond
          ignore?
          (do (log/debugf "%s ignored" (log-context))
              x)

          (and (nil? x) (some? default))
          (do (log/infof "%s not supported: returning provided default %s" (log-context) default)
              default)

          (not-any? #{target-type} supported-types)
          (do (log/warnf "%s not supported: unsupported target type, supported %s" (log-context) supported-types)
              x)

          (and (not= source-type target-type)
               (and (not= ::unsupported source-type)
                    (not= ::unsupported target-type)))
          (do (log/infof "%s supported" (log-context))
              (muotti/transform transformer source-type target-type x))

          :else
          (do (log/errorf "%s unhandled: not sure what to do. %s" (log-context) {})
              x)))))

; thank you, random comment on Stackoverflow https://stackoverflow.com/a/12433266/44523
(defn ^:private get-meta [o]
  (->> *ns*
       ns-map
       (filter (fn [[_ v]] (and (var? v) (= o (var-get v)))))
       first
       second
       meta))

(defn ^:private symbolize
  "Convert function names to symbols."
  [maybe-fns]

  (map (fn [v] (if (fn? v) (-> (get-meta v) :name) v)) maybe-fns))
; TODO: Ei toimi, tsekkaa REPL-sessio
; https://stackoverflow.com/a/12433266/44523
; (symbol (name (quote pos?)))

(defn ^:private ->malli-xformer
  [muotti-transformer from-fn to-fn]
  (fn [schema _options]
    (converter
      muotti-transformer
      (partial from-fn schema)
      (partial to-fn schema)
      (malli/-properties schema)
      (->> (muotti/config muotti-transformer) :transformations keys flatten symbolize set))))

(defn transformer
  ([muotti-transformer]
   (transformer muotti-transformer muotti-transformer))
  ([muotti-decoder muotti-encoder]
   (letfn [(from-schema [schema _] (some-> schema (malli/-parent) (malli/-type)))]
     (mt/transformer
       {:name            :muotti
        :default-decoder {:compile (->malli-xformer muotti-decoder detect-type from-schema)}
        :default-encoder {:compile (->malli-xformer muotti-encoder from-schema detect-type)}}))))

;; XXX: similar to but not exactly as malli.core/-register-var
(defn ^:private register-pred
  [registry pred schema]
  (assoc registry (-> pred meta :name) schema @pred schema))

(defn extension-types-registry
  [base-registry]
  (let [instance?-schema (malli/-simple-schema
                           (fn [opts _]
                             {:type            (:muotti/type opts)
                              :pred            #(instance? (:class opts) %)
                              :type-properties {:error/fn (fn [error _] (str "Expected value to be " type ", was " (:value error) " instead"))}}))
        custom-predicates (-> {}
                              (register-pred #'instance? instance?-schema))]
    (mr/composite-registry
      base-registry
      custom-predicates
      {::big-int     (malli/schema [instance? {:muotti/type ::big-int :class BigInt}] {:registry custom-predicates})
       ::big-integer (malli/schema [instance? {:muotti/type ::big-integer :class BigInteger}] {:registry custom-predicates})
       ::big-decimal (malli/schema [instance? {:muotti/type ::big-decimal :class BigDecimal}] {:registry custom-predicates})})))
