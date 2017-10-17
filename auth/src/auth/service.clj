(ns auth.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [korma.db :as kdb]
            [hiccup.core :as h]
            [hiccup.page :as hp]
            [hiccup.form :as hf]))

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
                        [:form {:action "/auth"
                                :method :POST}
                         [:div.form-group
                          [:label "D-BAS Account"]
                          [:input.form-control {:name "account"}]]
                         [:div.form-group
                          [:label "Password"]
                          [:input.form-control {:name "password" :type :password}]]
                         [:input {:name "redirect_uri" :value redirect_uri :type :hidden}]
                         [:input {:name "account_linking_token" :value account_linking_token :type :hidden}]
                         [:input {:class "btn btn-primary"
                                  :type :submit}]]
                        [:p "redirect_uri: " redirect_uri]
                        [:p "account_linking_token: " account_linking_token]])))

(defn auth-page [{{:keys [account]} :form-params :as request}]
  (ring-resp/response (hp/html5
                       (hp/include-css "css/bootstrap.min.css")
                       [:div.container
                        [:h1 "Auth"]
                        [:p (:form-params request)]
                        [:p (str account)]
                        [:p (str request)]])))



(defn home-page [request]
  (ring-resp/response "Hello World!"))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/login" :get (conj common-interceptors `login-page)]
              ["/auth" :post (conj common-interceptors `auth-page)]})

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

