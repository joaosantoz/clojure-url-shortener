(ns url-shortener.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [url-shortener.core :as core]))

(deftest parse-port-test
  (testing "aceita portas validas"
    (is (= 8080 (core/parse-port "8080")))
    (is (= 1 (core/parse-port "1")))
    (is (= 65535 (core/parse-port "65535")))
    (is (= 3000 (core/parse-port "  3000  ")) "deve ignorar espacos em volta"))

  (testing "cai para a porta padrao quando o valor nao serve"
    (is (= core/porta-padrao (core/parse-port nil)))
    (is (= core/porta-padrao (core/parse-port "")))
    (is (= core/porta-padrao (core/parse-port "abc")))
    (is (= core/porta-padrao (core/parse-port "80.5"))))

  (testing "cai para a porta padrao quando o numero esta fora da faixa"
    (is (= core/porta-padrao (core/parse-port "0")))
    (is (= core/porta-padrao (core/parse-port "-1")))
    (is (= core/porta-padrao (core/parse-port "65536")))))
