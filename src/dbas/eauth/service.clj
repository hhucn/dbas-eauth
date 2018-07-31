(ns dbas.eauth.service
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :as async :refer [<!!]]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [konserve.filestore :refer [new-fs-store]]
            [konserve.core :as k]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [hiccup.page :as hp]))

(def dbas-url (or (System/getenv "DBAS_URL") "https://dbas.cs.uni-duesseldorf.de/api/login"))
(def store (<!! (new-fs-store (or (System/getenv "EAUTH_STORE") "./store"))))

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
      (client/post dbas-url
                   {:body (json/write-str {:nickname account :password password})})
      (println "Login successful. Redirecting to" redirect)
      (swap! users_auth assoc uuid account)
      (ring-resp/redirect redirect)
      (catch Exception e
        (println "Login not successful. Redirecting to" redirect_uri)
        (println e)
        (ring-resp/redirect redirect_uri)))))

(defn success-page [{{:keys [recipient sender account_linking]} :json-params :as params}]
  (when (s/valid? ::success-params (:json-params params))
    (let [auth (:authorization_code account_linking)
          status (:status account_linking)
          nickname (get @users_auth auth)]
      (case status
        "linked" (do
                   (<!! (k/assoc-in store [{:service "facebook"
                                            :app_id (:id recipient)
                                            :user_id (:id sender)}]
                                          nickname))
                   (swap! users_auth dissoc auth)
                   (println nickname "linked")
                   (http/json-response {:status :ok :message "User logged in"}))
        "unlinked" (do
                     (<!! (k/dissoc store {:service "facebook" :app_id (:id recipient) :user_id (:id sender)}))
                     (println "User unlinked")
                     (http/json-response {:status :ok :message "User logged out"}))))))

(defn resolve-user [{{:keys [service app_id user_id]} :params :as request}]
  (if (s/valid? ::resolve-user-params (:params request))
    (if-let [nickname (<!! (k/get-in store [{:service service
                                             :app_id app_id
                                             :user_id user_id}]))]
      (http/json-response {:status :ok, :data {:nickname nickname}})
      (http/json-response {:status :error, :data "Could not resolve nickname!"}))
    (http/json-response {:status :error, :data "You fucked up your parameters! Try: /resolve-user?service=Facebook&app_id=1456&user_id=2378"})))

(comment
  (client/get "http://localhost:8080/resolve-user?service=Facebook&app_id=1144092719067446&user_id=1235572976569567"))

(defn add-user [{json :json-params}]
    (if (s/valid? ::add-user-params json)
      (do (k/assoc-in store [(select-keys json [:service :app_id :user_id])] (:nickname json))
          (http/json-response {:status :ok}))
      (http/json-response {:status :error})))

;; -----------------------------------------------------------------------------

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

(s/def ::service string?)
(s/def ::app_id string?)
(s/def ::user_id string?)

(s/def ::resolve-user-params
  (s/keys :req-un [::service ::app_id ::user_id]))

(s/def ::add-user-params
    (s/keys :req-un [::service ::app_id ::user_id ::nickname]))

;; -----------------------------------------------------------------------------

(def common-interceptors [(body-params/body-params) http/html-body])

(def routes #{["/login" :get (conj common-interceptors `login-page)]
              ["/auth" :post (conj common-interceptors `auth-page)]
              ["/success" :post (conj common-interceptors `success-page)]
              ["/resolve-user" :get (conj common-interceptors `resolve-user)]
              ["/add-user" :post (conj common-interceptors `add-user)]})

(def service {:env :prod
              ::http/routes routes
              ::http/resource-path "/public"
              ::http/type :jetty
              ::http/port 1236
              ::http/container-options {:h2c? true
                                        :h2? false
                                        :ssl? false}})
