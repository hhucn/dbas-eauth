(ns auth.service-test
  (:require [clojure.test :refer :all]
            [io.pedestal.test :refer :all]
            [io.pedestal.http :as bootstrap]
            [auth.service :as service]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(deftest page-availability-test
  (are [x y] (= x y)
    200 (:status (response-for service :get "/login"))
    302 (:status (response-for service :post "/auth"))))
