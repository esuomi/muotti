
> # [_**Muotti**_](https://en.wiktionary.org/wiki/muotti)<br />
> _**Noun.**_
>
> 1. mould/mold (hollow form or matrix for shaping a fluid or plastic substance)
> 2. cast (mould used to make cast objects)
> 3. die
> 4. form (thing that gives shape to other things as in a mold)

<sup>(_source: https://en.wiktionary.org/wiki/muotti_)</sup>

[![Deploy to Clojars](https://github.com/esuomi/muotti/actions/workflows/deploy.yaml/badge.svg)](https://github.com/esuomi/muotti/actions/workflows/deploy.yaml)
[![Clojars Project](https://img.shields.io/clojars/v/fi.polycode/muotti.svg)](https://clojars.org/fi.polycode/muotti)
[![cljdoc badge](https://cljdoc.org/badge/fi.polycode/muotti)](https://cljdoc.org/jump/release/fi.polycode/muotti)


Muotti is a graph based value transformer library which aims
to solve value transformation by utilizing a [digraph](https://en.wikipedia.org/wiki/Directed_graph) of known
transformations to produce a _transformer chain_ which is then used to perform the actual transformation.


> # Archival note
> This library is no longer maintained, but remains functional. Its latest published version can be considered feature complete and has proven to be stable. If you need to modify the library in any way, do consider [forking](https://github.com/esuomi/muotti/fork).

# Usage

Given a map of adjacencies - that is, edges of a graph - with validation and transformer functions:
```clojure
(require '[muotti.core :as muotti])

(def config {:transformations {[:keyword :string] {:validator   keyword?
                                                   :transformer name}
                               [:string :number]  {:validator   string?
                                                   :transformer parse-long}
                               [:string :boolean] {:validator   string?
                                                   :transformer boolean}
                               [:number :string]  {:validator   number?
                                                   :transformer str}}})
```

a transformer can be created:
```clojure
(def t (muotti/->transformer config))
```

which is then immediately usable for transforming values:
```clojure
(muotti/transform t :keyword :number :123)
; => 123
(muotti/transform t :number :boolean 123)
; => true  ;; non-empty values are treated as ´true´ by clojure.core/boolean
```

Unresolvable transformations return a special value:
```clojure
(muotti/transform t :keyword :double :3.14)
; => ::unknown-path
```

Transformer chain validation errors also return a special value:
```clojure
(def broken-adjacency {:transformations {[:a :b] {:validator   keyword?
                                                  :transformer str}}})
(def t2 (muotti/->transformer broken-adjacency))
(muotti/transform t2 :a :b "not a number")
;; => ::invalid-value
```

All possible paths in the graph will be tested to resolve a result:
```clojure
(def multiple {:transformations {[:in :num] {:transformer #(Integer/parseInt %)}
                                 [:in :str] {:transformer str}
                                 [:str :out] {:transformer #(= "magic!" %)}
                                 [:num :out] {:transformer #(= 6 %)}}})
(def t3 (muotti/->transformer multiple))
(muotti/transform t3 :in :out "6")
;;=> true
(muotti/transform t3 :in :out "magic!")
;;=> true
(muotti/transform t3 :in :out "0")
;;=> false
(muotti/transform t3 :in :out "anything")
;;=> false
```

> Resolving order of paths is not guaranteed to be stable!

## [Malli](https://github.com/metosin/malli) integration

Muotti is made to complement Malli's decoding and encoding capabilities through [Malli's Value Transformation](https://github.com/metosin/malli#value-transformation)
capability.

Create a Malli transformer and then use it to call eg. `malli.core/decode` with the transformer:
```clojure
(require '[malli.core :as malli])
(require '[muotti.malli :as mm])

(def malli-transformer (mm/transformer (muotti/->transformer mm/malli-config)))

(malli/decode
  [:map
   [:a {:muotti/ignore true} :uuid]
   [:b :int]]
  {:a :invalid
   :b "123"}
  malli-transformer)
;;=> {:a nil, :b 123}
```

### Override source and target types

Use `:muotti/source` and `:muotti/target` properties to override transformation types.

See [muotti.malli-tests/override-types](./src/test/clj/muotti/malli_tests.clj#L76) for examples.

### Provide default value for `nil` inputs

Use `:muotti/default` to provide a default value.

```clojure
(malli/decode
  [:string {:muotti/default "hello"}]
  nil
  malli-transformer)
;;=> hello
```

### Supported transformations

Muotti's aim is to support all major Malli types and predicates which are too numerous to list here. Instead see either
 1. [`muotti.malli-tests`](src/test/clj/muotti/malli_tests.clj) namespace or
 2. The DOT graph below

## [DOT/GraphViz](https://graphviz.org/) support

It is possible to output the graph contained by the transformer as DOT:
```clojure
(->> (muotti/->transformer mm/malli-config)
     (muotti/visualize-dot)
     (spit "/tmp/graph.dot"))
```
The resulting file can be input into [GraphViz](https://graphviz.org/):
```shell
dot -Tpng /tmp/graph.dot > graph.png
```
which results in
![DOT example output](./docs/images/graph.png)

## [Mermaid Flowchart](https://mermaid.js.org/syntax/flowchart.html) support

For easier embedding the graph can also be converted into a Mermaid Flowchart script:
```clojure
(->> (muotti/->transformer mm/malli-config)
     (muotti/visualize-mermaid)
```
This results in a string which can be, for example, put directly into GitHub Markdown file:
```mermaid
flowchart TD
	:qualified-symbol([:qualified-symbol]) --> :string([:string])
	:qualified-symbol([:qualified-symbol]) --> :any([:any])
	:double([:double]) --> float?([float?])
	:double([:double]) --> double?([double?])
	:double([:double]) --> :string([:string])
	:double([:double]) --> number?([number?])
	:double([:double]) --> :any([:any])
	:muotti.malli/big-integer([:muotti.malli/big-integer]) --> integer?([integer?])
	:muotti.malli/big-integer([:muotti.malli/big-integer]) --> nat-int?([nat-int?])
	:muotti.malli/big-integer([:muotti.malli/big-integer]) --> number?([number?])
	:int([:int]) --> :double([:double])
	:int([:int]) --> :muotti.malli/big-integer([:muotti.malli/big-integer])
	:int([:int]) --> int?([int?])
	:int([:int]) --> :muotti.malli/ratio([:muotti.malli/ratio])
	:int([:int]) --> :muotti.malli/float([:muotti.malli/float])
	:int([:int]) --> :string([:string])
	:int([:int]) --> :muotti.malli/big-decimal([:muotti.malli/big-decimal])
	:int([:int]) --> integer?([integer?])
	:int([:int]) --> nat-int?([nat-int?])
	:int([:int]) --> number?([number?])
	:int([:int]) --> :any([:any])
	:symbol([:symbol]) --> :string([:string])
	:symbol([:symbol]) --> :any([:any])
	:qualified-keyword([:qualified-keyword]) --> :string([:string])
	:qualified-keyword([:qualified-keyword]) --> :any([:any])
	int?([int?]) --> neg-int?([neg-int?])
	int?([int?]) --> pos-int?([pos-int?])
	:muotti.malli/ratio([:muotti.malli/ratio]) --> ratio?([ratio?])
	:muotti.malli/ratio([:muotti.malli/ratio]) --> number?([number?])
	:muotti.malli/float([:muotti.malli/float]) --> float?([float?])
	:muotti.malli/float([:muotti.malli/float]) --> number?([number?])
	:string([:string]) --> :double([:double])
	:string([:string]) --> :muotti.malli/big-integer([:muotti.malli/big-integer])
	:string([:string]) --> :int([:int])
	:string([:string]) --> :symbol([:symbol])
	:string([:string]) --> :muotti.malli/ratio([:muotti.malli/ratio])
	:string([:string]) --> :keyword([:keyword])
	:string([:string]) --> :uuid([:uuid])
	:string([:string]) --> :boolean([:boolean])
	:keyword([:keyword]) --> :symbol([:symbol])
	:keyword([:keyword]) --> :string([:string])
	:keyword([:keyword]) --> :any([:any])
	:muotti.malli/big-decimal([:muotti.malli/big-decimal]) --> decimal?([decimal?])
	:muotti.malli/big-decimal([:muotti.malli/big-decimal]) --> float?([float?])
	:muotti.malli/big-decimal([:muotti.malli/big-decimal]) --> number?([number?])
	integer?([integer?]) --> rational?([rational?])
	ratio?([ratio?]) --> rational?([rational?])
	:uuid([:uuid]) --> :string([:string])
	:uuid([:uuid]) --> :any([:any])
	:boolean([:boolean]) --> :string([:string])
	:boolean([:boolean]) --> :any([:any])
	number?([number?]) --> pos?([pos?])
	number?([number?]) --> neg?([neg?])
	number?([number?]) --> zero?([zero?])
```
