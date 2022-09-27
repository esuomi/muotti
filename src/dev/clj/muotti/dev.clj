(ns muotti.dev
  (:require [malli.core :as malli]
            [malli.transform :as mt]
            [muotti.core :as muotti]
            [muotti.malli :as mm]))

(def adjacencies {[:keyword :string]  {:validator   keyword?
                                       :transformer name}
                  [:keyword :boolean] {:validator   keyword?
                                       :transformer boolean}
                  [:string :number]   {:validator   string?
                                       :transformer parse-long}
                  [:string :boolean]  {:validator   string?
                                       :transformer boolean}
                  [:number :keyword]  {:validator   number?
                                       :transformer (comp keyword str)}
                  [:number :boolean]  {:validator   number?
                                       :transformer boolean}})

#_((malli/decoder
   [:map
    [:x [:set [:enum {:muotti/default "S"} "S" "L"]]]
    [:y {:muotti/ignore true} :uuid]
    [:z [:tuple :boolean [:map [:a :int]]]]]
   (mm/transformer (->transformer mm/malli-config)))
 {:x #{"S" "L" "XL" nil}
  :y :invalid
  :z [true {:a 123}]})
; TODO: document :validator being optional
