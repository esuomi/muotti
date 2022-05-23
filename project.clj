(defproject muotti "_"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.11.0"]

                 ;Ecosystem friendly logging
                 [org.slf4j/slf4j-api "1.7.36"]
                 [ch.qos.logback/logback-classic "1.2.11"]
                 [org.clojure/tools.logging "1.2.4"]
                 [io.aviso/pretty "1.1.1"]                  ; pretty exceptions
                 [mvxcvi/puget "1.3.2"]                     ; pretty everything else

                 [metosin/malli "0.8.4"]
                 [org.babashka/sci "0.3.5"]

                 ; rich type helper libraries
                 [lambdaisland/uri "1.13.95"]
                 ]

  :plugins [[fi.polycode/lein-git-revisions "1.0.0"]
            [lein-ancient "1.0.0-RC3"]
            [nvd-clojure "2.2.0"]]

  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"]
  :global-vars {*warn-on-reflection* true}

  :source-paths ["src/main/clj"]
  :test-paths ["src/test/clj"]
  :resource-paths ["src/main/resources"]
  :target-path "target/%s"

  :middleware [whidbey.plugin/repl-pprint]

  :profiles {:dev {:dependencies   [[org.clojure/tools.namespace "1.3.0"]
                                    [io.aviso/pretty "1.1.1"]
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
                  :adjust        [:env/project_revision_adjustment :minor]
                  :revision-file "resources/metadata.edn"})
