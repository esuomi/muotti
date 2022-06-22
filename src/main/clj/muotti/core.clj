(ns muotti.core
  (:require [loom.alg :as alg]
            [loom.attr :as attr]
            [loom.graph :as graph]
            [loom.io :as lio]
            [clojure.tools.logging :as log]))

(defn ^:private adjacencies-as-graph
  "Converts given adjacencies data structure to fully populated digraph."
  [adjacencies]
  (reduce
    (fn [g [link attrs]]
      (reduce
        (fn [g [k v]]
          (attr/add-attr-to-edges g k v [link]))
        g
        attrs))
    (apply graph/digraph (keys adjacencies))
    adjacencies))

(defn ^:private resolve-transformer-chain
  [graph from to]
  (let [path (alg/bf-path graph from to)]
    (if (some? path)
      (do
        (log/debugf "Resolved transformation [%s -> %s] as path %s" from to path)
        (->> (partition 2 1 path)
             (map (fn [[from to]]
                    (attr/attrs graph from to)))
             (filter some?)))
      (do
        (log/warnf "Transformation [%s -> %s] cannot be resolved with given adjancency list" from to)
        ::unknown-path))))

(def default-config
  "Collection of default type transformations.

  See also [[transform]]"
  {:transformations {[::keyword ::string] {:validator keyword? :transformer name}
                     [::string ::long]    {:validator string?  :transformer parse-long}
                     [::string ::double]  {:validator string?  :transformer parse-double}
                     [::string ::boolean] {:validator string?  :transformer parse-boolean}
                     [::string ::keyword] {:validator string?  :transformer keyword}
                     [::long ::double]    {:validator int?     :transformer double}
                     [::long ::boolean]   {:validator int?     :transformer boolean}
                     [::long ::string]    {:validator int?     :transformer str}
                     [::double ::boolean] {:validator double?  :transformer boolean}
                     [::double ::string]  {:validator double?  :transformer str}
                     [::boolean ::string] {:validator boolean? :transformer str}}})

(defprotocol Transformer
  "Transformer defines an encapsulated relation between possible transformation steps represented as adjacency list, the
   digraph they form and user space API for working with the data."
  (transform [this from to value]
    "Tries to transform given `value` between given types. The type pair `from` and `to` does not have to be directly
    known but must be resolvable into a transformation chain.

    If path between the types is not found, special value `::unknown-path` is returned.
    If value cannot be transformed with the resolved chain, special value `::invalid-value` is returned.")
  (graph-dot [this]
    "Render the contained graph out as DOT document. Returns the result as string.")
  (config [this]
    "Return the original configuration this transformer was created with."))

; TODO: logging, better chain debugging, malli walks

(defn ->transformer
  "Creates a new [[Transformer]] instance from given map of adjacencies and related configuration. By default uses
  [[default-adjacencies]], but the map of adjacency lists to validation and transformation functions can be overridden.

  ```clojure
  ; create new transformer with built-in defaults:
  (def defaults-t (->transformer))

  ; create new transformer with custom transformation configuration:
  (def custom-t (-> transformer {:transformations [:keyword :string] {:validator   keyword?
                                                                      :transformer name}
                                                  [:string :number]  {:validator   string?
                                                                      :transformer parse-long}})
  ```"
  ([] ->transformer default-config)
  ([{:keys [transformations]
     :as   config}]
   (let [graph (adjacencies-as-graph transformations)]
     (reify Transformer
       (transform [_ from to value]
         (let [chain (resolve-transformer-chain graph from to)]
           (if (= ::unknown-path chain)
             chain
             (reduce
               (fn [v {:keys [validator transformer]}]
                 (if (if (some? validator)
                       (validator v)
                       true)
                   ; TODO: logging/exception handling for actual transformation
                   (transformer v)
                   (do
                     (log/warnf "Value %s cannot be validated with the user provided validator %s" v validator)
                     (reduced ::invalid-value))))
               value
               chain))))
       (graph-dot [_]
         (lio/dot-str graph))
       (config [_]
         config)))))

; TODO: When logging value v, wrap with check that allows hiding it from logs, eg. :muotti/guard
