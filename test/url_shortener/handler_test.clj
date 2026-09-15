(ns url-shortener.handler-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [url-shortener.handler :as handler]
            [url-shortener.store :as store])
  (:import [java.io ByteArrayInputStream]))

(defn- json-stream
  [m]
  (ByteArrayInputStream. (.getBytes (json/generate-string m) "UTF-8")))

(defn- request
  ([method uri] (request method uri nil))
  ([method uri body]
   (cond-> {:request-method method
            :uri            uri
            :scheme         :http
            :headers        {"host" "localhost:3000"}}
     body (assoc :body (json-stream body)))))

(defn- body->map
  [response]
  (json/parse-string (:body response) true))

(defn- new-app []
  (handler/app (store/new-store)))

(deftest health-test
  (testing "GET /health responde 200"
    (let [resposta ((new-app) (request :get "/health"))]
      (is (= 200 (:status resposta)))
      (is (= "ok" (:status (body->map resposta))))
      (is (= 0 (:links (body->map resposta)))))))

(deftest index-test
  (testing "GET / descreve o servico"
    (let [resposta ((new-app) (request :get "/"))
          corpo    (body->map resposta)]
      (is (= 200 (:status resposta)))
      (is (= "url-shortener" (:service corpo)))
      (is (seq (:endpoints corpo))))))

(deftest shorten-test
  (testing "POST /shorten cria um link novo"
    (let [app      (new-app)
          resposta (app (request :post "/shorten" {:url "https://pucpr.br"}))
          corpo    (body->map resposta)]
      (is (= 201 (:status resposta)))
      (is (= "https://pucpr.br" (:url corpo)))
      (is (= "0" (:id corpo)))
      (is (= "http://localhost:3000/0" (:short_url corpo)))
      (is (true? (:novo corpo)))))

  (testing "POST /shorten com a mesma URL devolve 200 e o mesmo codigo"
    (let [app      (new-app)
          _        (app (request :post "/shorten" {:url "https://pucpr.br"}))
          resposta (app (request :post "/shorten" {:url "https://pucpr.br"}))
          corpo    (body->map resposta)]
      (is (= 200 (:status resposta)))
      (is (= "0" (:id corpo)))
      (is (false? (:novo corpo)))))

  (testing "POST /shorten rejeita URL invalida"
    (let [resposta ((new-app) (request :post "/shorten" {:url "isso nao e uma url"}))]
      (is (= 400 (:status resposta)))
      (is (some? (:error (body->map resposta))))))

  (testing "POST /shorten sem corpo JSON valido devolve 400"
    (let [resposta ((new-app) (request :post "/shorten"))]
      (is (= 400 (:status resposta))))))

(deftest redirect-test
  (testing "GET /{codigo} redireciona para a URL original"
    (let [app      (new-app)
          _        (app (request :post "/shorten" {:url "https://clojure.org"}))
          resposta (app (request :get "/0"))]
      (is (= 302 (:status resposta)))
      (is (= "https://clojure.org" (get-in resposta [:headers "Location"])))))

  (testing "GET /{codigo} inexistente devolve 404"
    (let [resposta ((new-app) (request :get "/naoexiste"))]
      (is (= 404 (:status resposta))))))

(deftest links-test
  (testing "GET /links lista tudo o que foi encurtado"
    (let [app      (new-app)
          _        (app (request :post "/shorten" {:url "https://a.com"}))
          _        (app (request :post "/shorten" {:url "https://b.com"}))
          corpo    (body->map (app (request :get "/links")))]
      (is (= 2 (:total corpo)))
      (is (= "https://a.com" (get-in corpo [:links (keyword "0")]))))))

(deftest metodo-nao-permitido-test
  (testing "DELETE em qualquer rota devolve 405"
    (let [resposta ((new-app) (request :delete "/health"))]
      (is (= 405 (:status resposta))))))
