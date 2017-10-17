(defproject auth "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [io.pedestal/pedestal.service "0.5.3"]
                 [io.pedestal/pedestal.jetty "0.5.3"]

                 [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api]]

                 [hiccup "2.0.0-alpha1"]
                 [korma "0.4.0"]
                 [org.clojure/java.jdbc "0.7.1"]
                 [org.postgresql/postgresql "42.1.4"]

                 [org.slf4j/jul-to-slf4j "1.7.22"]
                 [org.slf4j/jcl-over-slf4j "1.7.22"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]]

  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]

  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "auth.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.3"]]
                   :plugins [[cider/cider-nrepl "0.16.0-SNAPSHOT"]
                             [refactor-nrepl "2.4.0-SNAPSHOT"]]}
             :uberjar {:aot [auth.server]}}
  :repl-options {:init (do (require 'auth.server)
                           (def the-server (auth.server/run-dev)))}
  :main ^{:skip-aot true} auth.server)

