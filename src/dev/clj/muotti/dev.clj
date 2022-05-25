(ns muotti.dev
  (:require [muotti.core :refer :all]))

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

