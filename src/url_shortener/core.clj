(ns url-shortener.core
  "Ponto de entrada da aplicacao."
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [url-shortener.handler :as handler]
            [url-shortener.store :as store]))

(defn- env-port
  "Porta lida da variavel de ambiente PORT (padrao 3000)."
  []
  (try
    (Integer/parseInt (or (System/getenv "PORT") "3000"))
    (catch NumberFormatException _
      (println "PORT invalida, usando 3000")
      3000)))

(defn -main
  [& _args]
  (let [port  (env-port)
        store (store/new-store)]
    (println (str "url-shortener v" handler/app-version
                  " ouvindo em http://0.0.0.0:" port))
    (jetty/run-jetty (handler/app store)
                     {:port port :host "0.0.0.0" :join? true})))
