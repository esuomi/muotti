(ns muotti.malli-tests
  (:require [clojure.test :refer :all]
            [malli.core :as malli]
            [muotti.core :as muotti]
            [muotti.malli :as mm]))

(defn ^:private create-transformer
  [config]
  (let [muotti-xformer (muotti/->transformer config)
        malli-xformer  (mm/transformer muotti-xformer)]
    malli-xformer))

(defn ^:private assert-decoding
  [label transformer malli-schema data expected]
  (testing label
    (is (= (malli/decode malli-schema data transformer) expected))))

(deftest malli-schema-support
  (let [tf (create-transformer mm/malli-config)]
    (testing "malli.core/type-schemas"
      (assert-decoding "[:string -> :int]" tf :int "123" 123)
      (assert-decoding "[:string -> :int] fails for decimal-like strings" tf :int "123.4" nil)
      (assert-decoding "[:string -> :double]" tf :double "123.4" 123.4)
      (assert-decoding "[:string -> :double] works for integer-like strings" tf :double "123" 123.0)
      (assert-decoding "[:string -> :boolean]" tf :boolean "true" true)
      (assert-decoding "[:string -> :boolean]" tf :boolean "false" false)
      (assert-decoding "[:string -> :boolean] is nil for 'stuff'" tf :boolean "stuff" nil)
      (assert-decoding "[:string -> :boolean] is nil for ''" tf :boolean "" nil)
      (assert-decoding "[:string -> :keyword]" tf :keyword "abc" :abc)
      (assert-decoding "[:string -> :keyword] allows creating visually confusing keywords" tf :keyword ":keyword" (keyword ":keyword"))  ; TODO: This might be a bug
      (assert-decoding "[:string -> :symbol]" tf :symbol "icon" 'icon)
      (assert-decoding "[:string -> :uuid]" tf :uuid "55f7b535-d87a-4635-ae46-a882291e4ae2" #uuid "55f7b535-d87a-4635-ae46-a882291e4ae2")
      (assert-decoding "[:int -> :string]" tf [:string] 1 "1")
      (assert-decoding "[:int -> :double]" tf [:double] 1 1.0)

      (assert-decoding "[:double -> :string]" tf :string 45.67 "45.67")

      (assert-decoding "[:boolean -> :string] converts true" tf :string true "true")
      (assert-decoding "[:boolean -> :string] converts false" tf :string false "false")
      (assert-decoding "[:boolean -> :string] nil is nil" tf :string nil nil)

      (assert-decoding "[:keyword -> :string]" tf :string :locknload "locknload")
      (assert-decoding "[:keyword -> :symbol]" tf :symbol :abloy 'abloy)
      (assert-decoding "[:qualified-keyword -> :string]" tf :string :lock/load "lock/load")

      (assert-decoding "[:symbol -> :string]" tf :string 'assa "assa")
      (assert-decoding "[:qualified-symbol -> :string]" tf :string 'fifty/sixty "fifty/sixty")

      (assert-decoding "[:uuid -> :string]" tf :string #uuid "479d9c47-64d6-4709-968d-c7d48aaf49a2" "479d9c47-64d6-4709-968d-c7d48aaf49a2")
      ; NOTES:
      ;  - :any is identity, so no tests at all for it
      ;  - :nil may be overridden by :muotti/default, so no specific tests for it
      )
    ; TODO: Verify all these work as expected
    (testing "malli.core/base-schemas"
      ; :and, :or, :orn, :not, :map, :map-of, :vector, :sequential, :set, :enum, :maybe, :tuple, :multi, :re, :fn, :ref, :=>, :function and :schema
      (assert-decoding ":string -> [:or :int :boolean] -> :int" tf [:or :int :boolean] "123" 123)
      (assert-decoding ":string -> [:or :int :boolean] -> :boolean" tf [:or :int :boolean] "true" true)
      (assert-decoding ":string -> [:or :int :boolean] -> nil" tf [:or :int :boolean] :remu :remu)

      (assert-decoding ":int -> [:and :int pos?] -> what" tf [:and :int pos?] 123 123)
      (assert-decoding ":int -> [:and :int pos?] -> what" tf [:and :int pos?] "456" 456)
      (assert-decoding ":int -> [:and :int pos?] -> what" tf [:and pos? :int] -789 -789)  ; muotti doesn't necessarily create valid data, it just forces types
      (assert-decoding ":int -> [:and :int pos?] -> what" tf [:and pos? :int] 789 789)
      )
    (testing "malli.core/predicate-schemas"

      ; any?, some?, number?, integer?, int?, pos-int?, neg-int?, nat-int?, pos?, neg?, float?, double?, boolean?, string?, ident?, simple-ident?, qualified-ident?, keyword?, simple-keyword?, qualified-keyword?, symbol?, simple-symbol?, qualified-symbol?, uuid?, uri?, decimal?, inst?, seqable?, indexed?, map?, vector?, list?, seq?, char?, set?, nil?, false?, true?, zero?, rational?, coll?, empty?, associative?, sequential?, ratio?, bytes?, ifn? and fn?
      )))

(deftest default-values
  (let [tf (create-transformer mm/malli-config)]
    (testing "provided default value is used for nil inputs"
      (assert-decoding "nil -> [:string {:muotti/default \"hello\"] -> hello" tf [:string {:muotti/default "hello"}] nil "hello"))))

(deftest override-types
  (let [tf (create-transformer {:transformations {[:override/source :string] {:transformer (constantly "overridden from")}
                                                  [:string :override/target] {:transformer (constantly "overridden to")}}})]
    (testing "source type can be overridden"
      (assert-decoding ":override/source -> :string" tf [:string {:muotti/source :override/source}] "value" "overridden from"))

    (testing "target type can be overridden"
      (assert-decoding ":string -> :override/target" tf [:string {:muotti/target :override/target}] "value" "overridden to"))))
