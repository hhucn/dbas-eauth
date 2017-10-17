(defproject auth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url  "https://choosealicense.com/licenses/mit/"}
  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [org.clojure/java.jdbc "0.7.1"]]
  :main ^:skip-aot auth.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
