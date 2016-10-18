(ns chatlogin.core
  (:use org.httpkit.server)
  (:use hiccup.core)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [taoensso.timbre :as timbre :refer [info  warn  error]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.core.async :refer [go]]
            [ring.util.response :refer [redirect]]
            [chatlogin.utils :refer [json-resp json-resp-error http-error html-resp uuid]]
            [environ.core :refer [env]])
  (:gen-class))


(def users (atom {}))


(def redirect-url (or (env :redirect-url) "http://localhost:8080/_oauth/jokelan"))

(defn page-template [title content]
  [:html
    [:head
      [:title title]
      [:link {:href "/styles.css" :rel "stylesheet"}]]
    [:body
      content]])

(defn username-form [uuid]
  [:div.login-page
    [:div.form
      [:form.login-form {:method "POST" :action "/authorize"}
        [:p "Kirjaudu sisään Joke-LANien chattiin syöttämällä haluamasi käyttäjänimi tai nick"]
        [:input {:name "username" :placeholder "Käyttäjänimi tai nick"}]
        [:input {:type "hidden" :name "uuid" :value uuid}]
        [:button "kirjaudu"]]]])


(defn authorize [req]
  (info req)
  (let [uid (uuid)
        state (get-in req [:query-params "state"])]
    (swap! users assoc uid {:state state})
    (html-resp (page-template "Username for chat" (username-form uid)))))


(defn authorize-set [req]
  (info req)
  (let [uid (get-in req [:params "uuid"])
        state (get-in @users [uid :state])
        username (get-in req [:params "username"])]
    (swap! users assoc uid {:username username :state state})
    (redirect (str redirect-url "?code=" uid "&state=" state))))


(defn log-and-ok [req]
  (info req)
  (json-resp {:ack "OK"}))

(defn token [req]
  (let [uid (get-in req [:form-params "code"])
        username (get-in @users [uid :username])]
    (json-resp {:access_token uid
                :token_type "bearer"
                :expires_in 259200000
                :refresh_token uid
                :uid username
                :id uid
                :info {:name username
                       :email (str uid "@lanittaja.lan")}})))


(defn me [req]
  (let [uid (get-in req [:params "access_token"])
        username (get-in @users [uid :username])]
    (json-resp  {:sub uid,
                 :id uid,
                 :name username,
                 :given_name username,
                 :family_name "joke-lan",
                 :preferred_username username,
                 :email (str uid "@lanittaja.lan"),
                 :picture "http://latimesblogs.latimes.com/.a/6a00d8341c630a53ef0133f563dee7970b-800wi"})))


(defroutes app
  (GET "/" [req] authorize)
  (POST "/token" [req] token)
  (GET "/me" [req] me)
  (GET "/authorize" [req] authorize)
  (POST "/authorize" [req] authorize-set)
  (route/resources "/")
  (route/not-found (json-resp-error (http-error 404 "not found"))))


(def server-options
  {:port (Integer/parseInt (or (env :port) "8082"))
   :ip (or (env :host) "0.0.0.0")
   :thread 12})


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (info "Starting chatlogin with settings" server-options)
  (run-server
    (-> app
      wrap-params
      (wrap-json-body {:keywords? true :bigdecimals? true}))
    server-options))
