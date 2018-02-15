(defproject dbas.eauth "0.0.2-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.9.0"]
                 [io.pedestal/pedestal.service "0.5.3"]
                 [io.pedestal/pedestal.jetty "0.5.3"]

                 [clj-http "3.7.0"]
                 [org.clojure/data.json "0.2.6"]

                 [hiccup "2.0.0-alpha1"]
                 [io.replikativ/konserve "0.4.11"]

                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]

                 [org.clojure/tools.logging "0.4.0"]]

  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]

  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "dbas.eauth.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.3"]]
                   :plugins [[cider/cider-nrepl "0.16.0-SNAPSHOT"]
                             [refactor-nrepl "2.4.0-SNAPSHOT"]]}
             :uberjar {:aot [dbas.eauth.server]}}
  :repl-options {:init (do (require 'dbas.eauth.server)
                           (def the-server (dbas.eauth.server/run-dev)))}
  :main ^{:skip-aot true} dbas.eauth.server)

