(ns auth.service
  (:require [clojure.spec.alpha :as s]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [korma.core :as kc]
            [korma.db :as kdb]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [hiccup.page :as hp]))

(kdb/defdb prod
  (kdb/postgres {:db (System/getenv "DATABASE_NAME")
                 :user (System/getenv "DATABASE_USER")
                 :password (System/getenv "DATABASE_PASS")
                 ;; optional keys
                 :host "db"
                 :port "5432"
                 :delimiters ""}))

(kc/defentity users
  (kc/entity-fields :id :nickname :service :app_id :user_id :created))

(def users_auth (atom {}))

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
      (println "Login successful. Redirecting to" redirect)
      (swap! users_auth assoc uuid account)
      (ring-resp/redirect redirect)
      (catch Exception _
        (println "Login not successful. Redirecting to" redirect_uri)
        (ring-resp/redirect redirect_uri)))))

(s/def ::id string?)
(s/def ::recipient (s/keys :req-un [::id]))
(s/def ::timestamp pos-int?)
(s/def ::sender (s/keys :req-un [::id]))
(s/def ::authorization_code string?)
(s/def ::status string?)
(s/def ::account_linking (s/keys :req-un [::status]
                                 :opt-un [::authorization_code]))
(s/def ::success-params
  (s/keys :req-un [::recipient ::timestamp ::sender ::account_linking]))

(defn success-page [{{:keys [recipient sender account_linking]} :json-params :as params}]
  (if (s/valid? ::success-params (:json-params params))
    (let [auth (:authorization_code account_linking)
          nickname (get @users_auth auth)]
      (kc/insert users (kc/values {:nickname nickname :service "Facebook" :app_id (:id recipient) :user_id (:id sender)}))
      (swap! users_auth dissoc auth)
      (ring-resp/response (hp/html5 [:h1 "Success!"])))))

;; {:recipient {:id 1144092719067446}, :timestamp 1509354888386, :sender {:id 1417200965011924},
;;  :account_linking {:authorization_code eaeb9728-5f99-48ba-816d-9f08917c5b99, :status linked}}

;; -----------------------------------------------------------------------------

(def common-interceptors [(body-params/body-params) http/html-body])

(def routes #{["/login" :get (conj common-interceptors `login-page)]
              ["/auth" :post (conj common-interceptors `auth-page)]
              ["/success" :post (conj common-interceptors `success-page)]})

(def service {:env :prod
              ::http/routes routes
              ::http/resource-path "/public"
              ::http/type :jetty
              ::http/port 8080
              ::http/container-options {:h2c? true
                                        :h2? false
                                        :ssl? false}})

