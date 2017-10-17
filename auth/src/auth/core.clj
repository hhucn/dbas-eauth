(ns auth.core
  (:gen-class))

(def db {:dbtype "postgresql"
         :dbname (System/getenv "DATABASE_NAME")
         :host "db"
         :user (System/getenv "DATABASE_USER")
         :password (System/getenv "DATABASE_PASS")
         :ssl true
         :sslfactory "org.postgresql.ssl.NonValidatingFactory"})

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
