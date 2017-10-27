(ns auth.service
  (:require [clojure.spec.alpha :as s]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [korma.db :as kdb]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [hiccup.page :as hp]))

(def db {:dbtype "postgresql"
         :dbname (System/getenv "DATABASE_NAME")
         :host "db"
         :user (System/getenv "DATABASE_USER")
         :password (System/getenv "DATABASE_PASS")
         :ssl true
         :sslfactory "org.postgresql.ssl.NonValidatingFactory"})

(kdb/defdb prod
  (kdb/postgres {:db (System/getenv "DATABASE_NAME")
                 :user (System/getenv "DATABASE_USER")
                 :password (System/getenv "DATABASE_PASS")
                 ;; optional keys
                 :host "db"
                 :port "5432"
                 :delimiters ""}))

;; -----------------------------------------------------------------------------

(defn login-page [{{:keys [redirect_uri account_linking_token]} :query-params}]
  (ring-resp/response (hp/html5
                       (hp/include-css "css/bootstrap.min.css")
                       [:div.container
                        [:h1 "Login"]
                        [:form {:action "/auth" :method :POST}
                         [:div.form-group
                          [:label "D-BAS Account"]
                          [:input.form-control {:name "account"}]]
                         [:div.form-group
                          [:label "Password"]
                          [:input.form-control {:name "password" :type :password}]]
                         [:input {:name "redirect_uri" :value redirect_uri :type :hidden}]
                         [:input {:name "account_linking_token" :value account_linking_token :type :hidden}]
                         [:input {:class "btn btn-primary"
                                  :type :submit}]]])))

(defn auth-page [{{:keys [account password redirect_uri]} :form-params}]
  (let [uuid (str (java.util.UUID/randomUUID))
        redirect (str redirect_uri "&authorization_code=" uuid)]
    (try
      (client/post "https://dbas.cs.uni-duesseldorf.de/api/login"
                   {:body (json/write-str {:nickname account :password password})})
      (ring-resp/redirect redirect)
      (catch Exception _
        (ring-resp/redirect redirect_uri)))))

(defn success-page [{{:keys [recipient sender account_linking]} :json-params :as params}]
  (println "\n\n\n\n\n\n\nSUCCESS ###################")
  (println {:params params})
  (ring-resp/response (hp/html5 [:h1 "Success!"])))

;; {:json-params {:recipient {:id 1144092719067446}, :timestamp 1509116849482, :sender {:id 1417200965011924}, :account_linking {:authorization_code f9db521f-574d-4df4-bc62-d1c6ba5abeae, :status linked}}

(s/def ::id string?)
(s/def ::messaging coll?)

;; -----------------------------------------------------------------------------

(def common-interceptors [(body-params/body-params) http/html-body])

(def routes #{["/login" :get (conj common-interceptors `login-page)]
              ["/auth" :post (conj common-interceptors `auth-page)]
              ["/success" :post (conj common-interceptors `success-page)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])


;; Consumed by auth.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port 8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})

