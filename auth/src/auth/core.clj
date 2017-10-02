(ns auth.core
  (:gen-class)
  (:require [environ.core :refer [env]]))

(def db {:dbtype "postgresql"
         :dbname (env :DATABASE_NAME)
         :host "db"
         :user (env :DATABASE_USER)
         :password (env :DATABASE_PASS)
         :ssl true
         :sslfactory "org.postgresql.ssl.NonValidatingFactory"})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
