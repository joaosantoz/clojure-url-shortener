(ns url-shortener.store-test
  (:require [clojure.test :refer [deftest is testing]]
            [url-shortener.store :as store]))

(deftest encode-base62-test
  (testing "converte inteiros para base62"
    (is (= "0" (store/encode-base62 0)))
    (is (= "9" (store/encode-base62 9)))
    (is (= "a" (store/encode-base62 10)))
    (is (= "Z" (store/encode-base62 61)))
    (is (= "10" (store/encode-base62 62)))
    (is (= "11" (store/encode-base62 63))))

  (testing "rejeita numeros negativos"
    (is (thrown? IllegalArgumentException (store/encode-base62 -1)))))

(deftest valid-url-test
  (testing "aceita http e https"
    (is (store/valid-url? "http://pucpr.br"))
    (is (store/valid-url? "https://pucpr.br/curso?id=1"))
    (is (store/valid-url? "HTTPS://PUCPR.BR")))

  (testing "rejeita entradas invalidas"
    (is (not (store/valid-url? "pucpr.br")))
    (is (not (store/valid-url? "ftp://pucpr.br")))
    (is (not (store/valid-url? "https://com espaco")))
    (is (not (store/valid-url? "")))
    (is (not (store/valid-url? nil)))
    (is (not (store/valid-url? 42)))))

(deftest shorten-test
  (testing "gera codigos sequenciais em base62"
    (let [s (store/new-store)]
      (is (= "0" (:id (store/shorten! s "https://a.com"))))
      (is (= "1" (:id (store/shorten! s "https://b.com"))))
      (is (= "2" (:id (store/shorten! s "https://c.com"))))
      (is (= 3 (store/count-links s)))))

  (testing "a mesma URL devolve sempre o mesmo codigo"
    (let [s          (store/new-store)
          primeira   (store/shorten! s "https://pucpr.br")
          repetida   (store/shorten! s "https://pucpr.br")]
      (is (true? (:created? primeira)))
      (is (false? (:created? repetida)))
      (is (= (:id primeira) (:id repetida)))
      (is (= 1 (store/count-links s)))))

  (testing "URL invalida devolve erro e nao altera o store"
    (let [s (store/new-store)]
      (is (= {:error :invalid-url} (store/shorten! s "isso nao e uma url")))
      (is (zero? (store/count-links s))))))

(deftest resolve-id-test
  (let [s  (store/new-store)
        id (:id (store/shorten! s "https://clojure.org"))]
    (testing "resolve um codigo existente"
      (is (= "https://clojure.org" (store/resolve-id s id))))

    (testing "devolve nil para codigo inexistente"
      (is (nil? (store/resolve-id s "naoexiste"))))))

(deftest list-links-test
  (let [s (store/new-store)]
    (store/shorten! s "https://a.com")
    (store/shorten! s "https://b.com")
    (is (= {"0" "https://a.com" "1" "https://b.com"}
           (store/list-links s)))))

(deftest encode-base62-unicidade-test
  (testing "nao repete codigos ao longo de uma faixa grande"
    (let [codigos (map store/encode-base62 (range 5000))]
      (is (= 5000 (count (distinct codigos))))
      (is (every? #(re-matches #"[0-9a-zA-Z]+" %) codigos))))

  (testing "o tamanho do codigo cresce junto com o contador"
    (is (= 1 (count (store/encode-base62 61))))
    (is (= 2 (count (store/encode-base62 62))))
    (is (= 2 (count (store/encode-base62 3843))))
    (is (= 3 (count (store/encode-base62 3844))))))

(deftest shorten-concorrente-test
  (testing "a mesma URL encurtada por varias threads gera um unico codigo"
    (let [s        (store/new-store)
          url      "https://www.pucpr.br"
          inicio   (java.util.concurrent.CountDownLatch. 1)
          tarefas  (doall (repeatedly 200 #(future (.await inicio)
                                                   (store/shorten! s url))))]
      (.countDown inicio)
      (let [ids (set (map (comp :id deref) tarefas))]
        (is (= 1 (count ids)) "todas as threads devem receber o mesmo codigo")
        (is (= 1 (store/count-links s)) "o store nao pode guardar duplicatas"))))

  (testing "URLs distintas em paralelo recebem codigos distintos"
    (let [s       (store/new-store)
          inicio  (java.util.concurrent.CountDownLatch. 1)
          tarefas (doall (for [i (range 200)]
                           (future (.await inicio)
                                   (store/shorten! s (str "https://exemplo.com/" i)))))]
      (.countDown inicio)
      (let [ids (set (map (comp :id deref) tarefas))]
        (is (= 200 (count ids)))
        (is (= 200 (store/count-links s)))))))
