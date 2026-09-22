(ns url-shortener.core
  "Ponto de entrada da aplicacao."
  (:gen-class)
  (:require [clojure.string :as str]
            [ring.adapter.jetty :as jetty]
            [url-shortener.handler :as handler]
            [url-shortener.store :as store]))

(def porta-padrao 3000)

(defn parse-port
  "Converte o valor bruto da variavel de ambiente PORT em uma porta valida.

  Devolve `porta-padrao` quando o valor e nulo, vazio, nao numerico ou esta
  fora da faixa 1-65535."
  [raw]
  (try
    (let [n (Integer/parseInt (str/trim (or raw "")))]
      (if (<= 1 n 65535) n porta-padrao))
    (catch Exception _ porta-padrao)))

(defn -main
  [& _args]
  (let [port  (parse-port (System/getenv "PORT"))
        store (store/new-store)
        opts  {:base-url (System/getenv "BASE_URL")}]
    (println (str "url-shortener v" handler/app-version
                  " ouvindo em http://0.0.0.0:" port))
    (jetty/run-jetty (handler/app store opts)
                     {:port port :host "0.0.0.0" :join? true})))
