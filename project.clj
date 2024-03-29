(defproject fi.polycode/muotti "_"
  :description "Muotti is a graph based value transformer library which aims to solve value transformation by utilizing
                a digraph of known transformations to produce a transformer chain which is then used to perform the
                actual transformation."
  :url "https://github.com/esuomi/muotti"
  :license {:name "MIT"
            :url  "https://choosealicense.com/licenses/mit"
            :comment "MIT License"
            :year 2023
            :key "mit"}

  :dependencies [[org.clojure/clojure "1.11.0"]

                 ;Ecosystem friendly logging
                 [org.slf4j/slf4j-api "2.0.6" :scope "provided"]
                 [ch.qos.logback/logback-classic "1.4.5" :scope "provided"]
                 [org.clojure/tools.logging "1.2.4"]

                 ;Graph library for Clojure. Eclipse Public License 1.0
                 [aysylu/loom "1.0.2"]

                 ; all muotti extensions are optional
                 [metosin/malli "0.10.0" :scope "provided"]]

  :deploy-repositories [["clojars" {:sign-releases false
                                    :url           "https://clojars.org/repo"
                                    :username      :env/CLOJARS_USERNAME
                                    :password      :env/CLOJARS_TOKEN}]]

  :plugins [[fi.polycode/lein-git-revisions "1.1.2"]
            [lein-ancient "1.0.0-RC3"]
            [nvd-clojure "2.2.0"]
            [lein-pprint "1.3.2"]
            [lein-kibit "0.1.8"]]

  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]
  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/main/clj"]
  :test-paths ["src/test/clj"]
  :resource-paths ["src/main/resources"]
  :target-path "target/%s"

  :middleware [whidbey.plugin/repl-pprint]

  :profiles {:dev {:dependencies   [[org.clojure/tools.namespace "1.3.0"]
                                    [io.aviso/pretty "1.3"]
                                    [com.gfredericks/test.chuck "0.2.13"]]
                   :plugins        [[io.aviso/pretty "1.1.1"]
                                    [mvxcvi/whidbey "2.2.1"]]
                   :middleware     [whidbey.plugin/repl-pprint
                                    io.aviso.lein-pretty/inject]
                   :source-paths   ["src/dev/clj"]
                   :resource-paths ["src/test/resources"]
                   :whidbey        {:print-fallback :print
                                    :escape-types   #{'clojure.lang.ExceptionInfo}}}}

  ; entrypoints
  :repl-options {:init-ns muotti.dev}

  :git-revisions {:format        :semver
                  :adjust        [:env/revisions_adjustment :minor]
                  :revision-file "src/main/resources/metadata.edn"})
